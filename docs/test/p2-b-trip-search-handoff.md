# P2-B — bàn giao GET /api/v1/trips

Ngày: 17/11/2026. Owner: Đỗ Minh Phương. Nhánh: `feat/p2-p4-trip-apis`.
Đây là bàn giao implementation/tests chưa commit, chưa thay approval của reviewer.

## Phạm vi và nguồn

- [OpenAPI](../contracts/openapi.yaml): operation `searchTrips`, `TripSearchPage`,
  `TripSearchItem`, parameters và error responses.
- [C1](../../pa/PA4/C1_DECISIONS.md), [quyết định P2/P4](../contracts/p2-p4-design-decisions.md),
  [manifest P1](p1-trip-demo-manifest.md), UC-P04/BR-P04-01..05.
- Phương duyệt bổ sung trong hội thoại: chỉ candidate còn có khả năng phù hợp
  nhưng không xác minh được mới gây 503; loại Trip chắc chắn không Scheduled,
  đã đóng bán hoặc sai ngày; mỗi leg toàn Trip và tổng Segment phải biểu diễn
  được trong `0..9007199254740991`, không cần cộng tổng toàn Trip.
- Không sửa P2-A, P3, Booking, security/common mapping, OpenAPI, SAD, migration,
  Q1/workbook, frontend hoặc dependency. Không triển khai P4.

## Luồng xử lý và dữ liệu

`TripSearchController` → `TripSearchRequestParser` → `TripSearchService`
→ `PostgresTripSearchRepository` → `TripSearchPolicy` → response page.

Parser kiểm tra required/duplicate scalar, UUID chuẩn, date và paging. Service
capture một Instant từ Clock inject riêng, dùng cho ngày Việt Nam, cutoff,
repository và serverTime. Toàn bộ các query nằm trong transaction read-only
REPEATABLE_READ; repository yêu cầu transaction caller với MANDATORY.

Repository dùng tối đa sáu query theo lô, không gọi adapter P3 theo từng Trip:

1. Served Stops: Stop active thuộc ít nhất một Route active, không phụ thuộc Trip.
2. Route active phục vụ cặp Stop theo đúng thứ tự.
3. Candidate Scheduled trên Route phục vụ cặp; LEFT JOIN giữ trường hợp thiếu
   TripStop/cutoff. Chỉ lọc ngày/cutoff khi có dữ liệu xác định được việc loại.
4. RouteStop/TripStop của mọi candidate, giữ cả Stop chưa được cấu hình cho Trip.
5. LegFare của mọi candidate, tách khỏi join ghế để không nhân tổng fare.
6. PhysicalSeat theo các Vehicle dùng chung.

Policy kiểm tra Stop order/giờ, Vehicle/layout và đầy đủ fare từng leg trên toàn
Trip trước pagination. Boarding dùng departure, alighting dùng arrival; không
fallback, không suy ngày từ trip.departure_date hoặc Schedule. Cutoff luôn là
departure Stop đầu trừ 30 phút; đúng mốc là đóng. Fare âm/quá safe integer hoặc
tổng Segment quá giới hạn gây 503; 0 hợp lệ. Giá chỉ cộng cho Segment được hỏi.

Sort theo boardingTime rồi UUID canonical text (cùng byte order PostgreSQL UUID).
Count/pages/items tính từ cùng tập đã xác minh, offset nhân bằng long và
totalPages dùng chia/lấy dư để tránh phép cộng tràn. Page vượt cuối vẫn giữ tổng.
Response có no-store; lỗi endpoint-local 400/422/503/500 không thay mapping C2.
Exception server được log, cause không được trả qua HTTP.

Giới hạn implementation: sau prefilter DB, facts của toàn bộ candidate còn có
khả năng khớp được đọc và kiểm tra trong bộ nhớ trước paging. Số query không
tăng theo số Trip, nhưng memory phụ thuộc kích thước tập này. Chưa benchmark
dữ liệu lớn; không tự thêm giới hạn candidate làm thay đổi contract.

## Dữ liệu thử và request

Fixture test cố định: `src/backend/src/test/resources/fixtures/trip_demo.sql`.
Clock IT: `2026-11-20T00:00:00Z` (07:00 VN). A/B/C và Trip IDs dùng manifest P1.

```text
GET /api/v1/trips?fromStopId=22222222-2222-4222-8222-222222222222&toStopId=99999999-9999-4999-8999-999999999999&boardingDate=2026-11-20&page=0&size=20
```

Với fixture/clock trên: hai Trip ngày/overnight, mỗi Segment A→C giá 250.000 VND;
boardingTime tăng dần. B→C ngày 21/11 chỉ trả overnight giá 150.000, cutoff vẫn
20/11 22:00 VN. Thiếu fare B→C mà hỏi A→B: 503, kể cả Trip hỏng nằm ngoài trang.
Không có Trip ngày phù hợp: 200/items rỗng; Stop chưa served/cặp sai: 422.

Các giá trị clock cố định chỉ dành cho tests; production dùng Clock.systemUTC.
Runtime seed/clock thật phải theo [hướng dẫn P1](p1-trip-runtime-seed-guide.md).

## Test coverage và đối chiếu Q1

| Q1 / nội dung | Test mới |
|---|---|
| TC-P04-01/02: kết quả thật, giá leg | PostgresTripSearchRepositoryIT, TripSearchPolicyTest |
| TC-P04-03: boarding date qua đêm | q1OvernightBoardingUsesVietnamDateNotUtcOrTripDepartureDate; overnightIntermediateBoardingStillClosesAtFirstStopCutoff |
| TC-P04-04: quá khứ theo VN | TripSearchServiceTest và emptyResultAndPastVietnamDateHaveDifferentOutcomes |
| TC-P04-05: served/Segment | servedStopAndOrderedPairValidationAreIndependentOfTrips; activeOrphanAndStopsOnlyOnInactiveRouteAreUnserved |
| TC-P04-06/07/08: rỗng, cutoff, status | Repository IT và policy tests; không áp expected Trip ngày còn bán chỉ từ điều kiện trước cutoff overnight |
| TC-P04-09: validation | TripSearchRequestParserTest và TripSearchControllerTest: missing, duplicate, format, paging |
| TC-P04-10: 503 | candidate ngoài trang thiếu fare, missing timestamps/layout/TripStop; realDatabaseAccessFailureIs503 dùng SET LOCAL search_path trong transaction test |
| Regression P3 | Cùng cutoff/whole-Trip fare completeness và giá 0; không đổi P3, không áp served/fallback mới vào P3. P3 không trả numeric fare nên tổng Segment đối chiếu C1/fixture. |
| Paging/biên | UUID tie-break qua bit cao, page vượt cuối/offset lớn, giá 0/max/quá max, tổng Segment quá max nhưng tổng toàn Trip không cần tính |
| Consistency | TripSearchSnapshotIT: reader/writer khác connection, latch/future chờ commit; service tự mở RR/read-only, không test-managed reader transaction |

Fare âm kiểm tra ở policy unit tests; V3 đã cấm ghi fare âm. Không sửa/drop
constraint để tạo dữ liệu sai. Controller tests mock service để kiểm tra HTTP;
Repository IT dùng PostgreSQL thật, service/repository thật và chỉ mock Clock.
SnapshotIT spy target repository chỉ điều phối interleaving, vẫn chạy SQL thật.
FE/UI chưa chạy; kết quả API/IT không thay kết quả functional UI của Q3.

## Các lượt kiểm tra thực sự đã chạy

Từ gốc repo:

```powershell
docker compose -f compose.test.yaml run --rm --no-deps backend-test sh ./mvnw -B -ntp test '-Dtest=TripSearch*Test' '-DfailIfNoTests=true'
docker compose -f compose.test.yaml run --rm backend-test
```

- Lượt unit/controller riêng: 54 tests, 0 failures/errors/skipped, BUILD SUCCESS.
- Lượt full đầu: Surefire 74 tests đạt; Failsafe 115 tests có 1 error ở bước
  stub SnapshotIT qua AOP proxy MANDATORY khi chưa có transaction. Không phải
  bằng chứng final GREEN. Đã sửa test dùng AopTestUtils lấy spy target, không
  thay production transaction và không giảm assertion.
- Chạy lại full cùng lệnh: Java 21.0.12.1, PostgreSQL 16.16; Surefire 74 và
  Failsafe 115 đều 0 failures/errors/skipped; BUILD SUCCESS, exit 0. Tổng 189.
  Kết thúc `2026-11-17T13:45:17Z` (20:45:17 VN).
- Riêng P2-B: parser 26, controller 9, service 5, policy 14, repository IT 21,
  snapshot IT 1 = 76 lượt test đạt. Không có skip.

XML/text reports nằm trong `src/backend/target/surefire-reports/` và
`failsafe-reports/` (gitignored). Lượt clean verify cuối thay reports lượt trước.
Sau verify dọn project test bằng `docker compose -f compose.test.yaml down`,
không `-v`, không dùng volume phát triển. `git diff --check` đã đạt.

## Phụ thuộc / còn chờ review

- Anonymous GET vẫn 401 vì SecurityConfiguration hiện tại; public acceptance
  BLOCKED chờ Hiếu/C2 mở đúng GET /api/v1/trips. Test dùng authenticated user
  không được tính là public acceptance; không tắt security filter.
- Error handlers chỉ scoped TripSearchController; Hiếu rà khi ghép C2, không
  có thay đổi common/security mapping trong lượt này.
- Q1 Case IDs/workbook/bằng chứng Q3 vẫn do Đức Hiếu xác nhận; không sửa Q1.
- Đã đối chiếu SHA256 11 file P2-A với đầu lượt: không file nào thay đổi.
- Chưa commit/push/merge/rebase hoặc chuyển nhánh. Dừng chờ Phương review.
