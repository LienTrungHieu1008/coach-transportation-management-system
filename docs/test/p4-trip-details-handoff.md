# P4 — bàn giao GET /api/v1/trips/{tripId}

Ngày: 18/11/2026 (giờ Việt Nam). Owner: Đỗ Minh Phương.
Nguồn P4: `feat/p4-trip-details`, commit `fb28ece3`, base `0115a8c`;
[PR #13](https://github.com/LienTrungHieu1008/coach-transportation-management-system/pull/13).
Bản ghép với H2 được bàn giao trên `fix/h2-public-seat-availability` qua
[PR #14](https://github.com/LienTrungHieu1008/coach-transportation-management-system/pull/14);
không sửa nhánh Phương. Các checkpoint dưới đây giữ kết quả tại thời điểm đã chạy.

## Contract hiện hành

Nguồn: [OpenAPI](../contracts/openapi.yaml), [C1](../../pa/PA4/C1_DECISIONS.md),
[quyết định P2/P4](../contracts/p2-p4-design-decisions.md),
[TripQueryPort](../contracts/trip-query-port.md), [manifest P1](p1-trip-demo-manifest.md).

- UUID path `tripId`; query `fromStopId`/`toStopId` cùng có hoặc cùng vắng.
  Sai format, blank, duplicate scalar hoặc thiếu một đầu cặp: 400 `INVALID_REQUEST`.
- Trip không tồn tại sau truy vấn thành công: 404 `TRIP_NOT_FOUND`.
  Cặp bằng nhau/đảo thứ tự hoặc Stop đã biết nhưng ngoài Trip: 422 `INVALID_SEGMENT`;
  Stop không tồn tại khi có đủ dữ liệu kết luận cặp sai: 422 `STOP_NOT_SERVED`.
  Thiếu cấu hình khiến không chứng minh được cặp: 200/segment null/essential incomplete.
- Trip tồn tại nhưng không Scheduled, đóng bán hoặc thiếu essential: 200 và verified
  partial details. Không biến lỗi DB thành dữ liệu vắng: retrieval failure là
  503 `TRIP_DATA_UNAVAILABLE`; lỗi bất ngờ là 500 `INTERNAL_ERROR`.
- Response giữ đúng required keys/nullability/schema, không thêm field. UTC kết thúc
  `Z`; success và các lỗi P4 đều `Cache-Control: no-store`. Không lộ SQL/stack trong body.
- Cutoff = departure Stop index 0 trừ 30 phút; đúng mốc là đóng. Không dùng Schedule,
  departure_date hoặc timestamp Stop giữa thay cutoff. Một server `asOf` cho eligibility
  và `serverTime`; không nhận clock từ client.
- Reasons không trùng, đủ mọi điều kiện đã xác định và theo thứ tự
  `TRIP_NOT_SCHEDULED`, `BOOKING_CLOSED`, `ESSENTIAL_DATA_INCOMPLETE`.
  Cutoff chưa xác minh thì không thêm `BOOKING_CLOSED`. Không chọn Segment không tự
  làm eligibility false; eligibility không bảo đảm còn ghế.
- Boarding dùng departure; alighting dùng arrival, không fallback. Arrival null ở Stop
  đầu/departure null ở Stop cuối không tự làm thiếu essential.

## Quyết định bổ sung được Phương duyệt trong task này

- V3 không có nguồn description: trả `description=null`, không thêm dữ liệu/migration.
- Missing TripStop: giữ membership/order xác minh từ RouteStop để có thể giữ Segment;
  timestamp chưa có trả null. Danh sách `stops` chỉ chứa TripStop thực sự có cấu hình;
  không tạo TripStop giả hoặc đánh lại index để che dữ liệu thiếu.
- Fare: xác minh essential legs trên toàn Trip; giá 0 hợp lệ, giá âm hoặc vượt
  `9007199254740991` không hợp lệ và không xuất trong `legFares`. Chỉ xuất các leg liên
  tiếp đã xác minh từ RouteStop và LegFare, không phụ thuộc sự tồn tại của TripStop,
  theo thứ tự Stop. Segment dùng leg thiếu/invalid thì giá null;
  Segment không dùng leg đó vẫn giữ giá hợp lệ. Cộng có kiểm tra safe integer trước
  `Math.addExact`; không cộng tổng toàn Trip không được hỏi.
- **Route/Stop inactive: Phương-approved proposal, pending Trung Hiếu confirmation.**
  Trip có thật vẫn 200, giữ facts/Segment/giá xác minh được; inactive Route hoặc bất kỳ
  Stop thuộc Trip làm eligibility false với `ESSENTIAL_DATA_INCOMPLETE`. Không thêm
  reason mới, không ghi Trung Hiếu đã duyệt quyết định này.

## Request đi qua đâu và dữ liệu thay đổi gì

`TripDetailsController` → `TripDetailsRequestParser` → `TripDetailsService`
→ `TripDetailsRepository` / `PostgresTripDetailsRepository` → `TripDetailsPolicy`
→ `TripDetailsResponse`.

Service mở transaction read-only `REPEATABLE_READ`; adapter dùng `MANDATORY`, cùng
DataSource/transaction manager. Facts, policy và response được tính từ cùng snapshot.
Không gọi strict P3 TripQueryPort hoặc P2 search để dựng partial details; không sửa P2/P3.
Tái dùng giới hạn tiền P2 và DTO/validation layout P3; regression tests đối chiếu cùng
fixture/asOf cho các quy tắc chung khi dữ liệu đầy đủ.

Tối đa bốn query khi Trip tồn tại: head Trip LEFT JOIN Route/Vehicle; RouteStop/Stop
LEFT JOIN TripStop; toàn bộ fare; PhysicalSeat của Vehicle. Không lọc status/cutoff/active
làm mất Trip; không join fare với ghế gây nhân dòng. Không query từng Stop/leg.
Cặp sai cần thêm một query Stop theo lô để phân biệt UUID không tồn tại với Stop ngoài Trip.
Trip không tồn tại kết thúc sau query head. Không khóa ghế hoặc gọi provider.

Ví dụ nhánh lỗi: xóa fare B–C trong DB test, hỏi A–B → vẫn 200/giá 100.000, reason
essential incomplete; hỏi A–C → Segment có giá null. Nếu truy vấn DB không thực hiện
được → 503 thay vì dùng các facts đã đọc làm response 200.

GET không ghi bảng Trip hoặc Booking, không tạo Booking/hold/SeatAllocation.
Không đổi contract HTTP/port P3, migration, dependency hoặc build/CI. Lượt triển khai
và review-fix ban đầu chưa sửa security; matcher public P4 được ghép tại checkpoint C2 bên dưới.

## File mới

Trong `src/backend/src/main/java/vn/edu/team11/coach/trip/`:

- `api/`: TripDetailsController, TripDetailsRequestParser, TripDetailsResponse,
  TripDetailsExceptionHandler (`.java`). Handler chỉ scoped controller P4.
- `application/`: TripDetailsService, TripDetailsRepository, TripDetailsModels,
  TripDetailsException (`.java`).
- `domain/TripDetailsPolicy.java`.
- `infrastructure/`: PostgresTripDetailsRepository, TripDetailsConfiguration (`.java`).

Trong `src/backend/src/test/java/vn/edu/team11/coach/trip/`:

- `TripDetailsTestData.java`.
- `api/TripDetailsRequestParserTest.java`, `api/TripDetailsControllerTest.java`.
- `application/TripDetailsServiceTest.java`, `domain/TripDetailsPolicyTest.java`.
- `infrastructure/PostgresTripDetailsRepositoryIT.java`, `infrastructure/TripDetailsSnapshotIT.java`.

Tài liệu mới: file handoff này. Trước checkpoint C2 không sửa file tracked hiện hữu;
không sửa fixture P2/P3. File bổ sung/thay đổi tại C2 được liệt kê riêng bên dưới.

## Đối chiếu Q1 P05 — coverage backend, không thay Q3/UI

| Q1 | Tests/bằng chứng |
|---|---|
| P05-01 | Controller strict JSON đủ keys, fare/stops/order thật trong PostgreSQL IT; regression với P2/P3 trên fixture đầy đủ. |
| P05-02 | Policy/controller/IT: absent pair, segment null, eligibility không tự false. |
| P05-03 | Parser/controller: unpaired cả hai chiều, UUID path/query sai, blank và duplicate scalar. |
| P05-04 | Service/IT/controller: Trip mất mới 404; không nhầm DB error thành 404. |
| P05-05 | Policy/IT: Departed/Completed/Cancelled; facts vẫn còn; đủ tám tổ hợp reasons ở policy. |
| P05-06 | Policy/IT: trước/đúng/sau cutoff, Segment B–C vẫn dùng first-stop cutoff, đối chiếu P3 khi đủ dữ liệu. |
| P05-07 | Missing TripStop/RouteStop/time/fare/layout; fare ngoài Segment; inactive Route/Stop; zero/max/invalid/overflow. |
| P05-08 | Description null hiện diện trong JSON; Trip đầy đủ vẫn allowed=true. |
| P05-09 | Cặp bằng nhau/đảo thứ tự/Stop ngoài Trip/UUID không tồn tại; thiếu cấu hình không tự kết luận invalid. |
| P05-10 | PostgreSQL IT đặt `SET LOCAL search_path TO pg_catalog` để gây lỗi SQL thật, đi qua service/controller → 503; unit/controller kiểm tra lỗi transaction và không lộ cause. |
| Bổ sung consistency | SnapshotIT: writer connection khác commit status, first departure, Stop name và fare sau head SELECT, trước các SELECT còn lại; reader giữ snapshot cũ, request mới thấy dữ liệu mới; SHOW isolation/read-only và PID được kiểm tra. Dùng latch/future, không sleep. |

P05-05 expected một reason chỉ áp dụng trước cutoff và đủ essential. P05-07 được tách
theo khả năng xác minh Segment/giá, không dùng expected "segment null hoặc giá null"
cho mọi dạng missing data. Không sửa Q1/workbook hoặc tự tạo Case ID chính thức.

## Kiểm chứng thực tế ngày 18/11/2026

Môi trường: Compose `team11-coach-test`, DB tmpfs riêng `team11_test`, Java 21.0.12.1,
PostgreSQL 16.16. Fixtures dùng ngày cố định 20/11/2026 theo manifest P1;
Clock `2026-11-20T00:00:00Z`, cutoff Trip ngày `2026-11-20T01:30:00Z`.
Test fare âm ở policy vì V3 cấm lưu giá âm; oversized leg và overflow được kiểm chứng
trên PostgreSQL thật. Không sửa/drop constraints để tạo dữ liệu test.

Lệnh từ gốc repo:

```powershell
docker compose -f compose.test.yaml run --rm backend-test sh ./mvnw -B -ntp verify '-Dtest=TripDetails*Test' '-Dit.test=PostgresTripDetailsRepositoryIT,TripDetailsSnapshotIT' -DfailIfNoTests=true
docker compose -f compose.test.yaml run --rm backend-test
docker compose -f compose.test.yaml down
```

- Focused verify trước review-fix: BUILD SUCCESS, exit 0, 54 Surefire + 24 Failsafe = **78 tests**,
  0 failures/errors/skipped; kết thúc `2026-11-18T06:21:24Z` (13:21:24 VN).
- Chi tiết P4 trước review-fix: controller 11, parser 8, service 4, policy 31, repository IT 23,
  snapshot IT 1. Các lớp đều không skip.
- Full backend clean verify trước review-fix: BUILD SUCCESS, exit 0; **277 Surefire + 263 Failsafe
  = 540 tests**, 0 failures/errors/skipped. Kết thúc `2026-11-18T06:23:35Z`
  (13:23:35 VN). Đã đối chiếu tổng và sáu lớp test P4 từ XML reports của lượt full;
  riêng P4 vẫn 78/78 PASS. Không suy tổng từ số liệu checkpoint P2 trước đây.
- Đã chạy `docker compose -f compose.test.yaml down`, exit 0, không `-v`; chỉ dọn
  project test riêng, không dùng/xóa volume phát triển. Maven có cảnh báo metadata
  repository ngoài (401/DNS) nhưng hai lượt verify cuối đều kết thúc SUCCESS.
- `git diff --check` đạt; các file mới untracked được kiểm tra whitespace riêng
  bằng `git diff --no-index --check` so với `/dev/null` (bỏ cảnh báo chuyển LF/CRLF
  bằng tùy chọn Git chỉ áp dụng cho lệnh kiểm tra, không đổi config repo).
- Lịch sử trước focused GREEN: Docker ban đầu chưa bật; sau khi khởi động Docker,
  lượt đầu lỗi compile test do import DATE trùng; lượt kế lỗi hai ca restub Mockito
  đang ném exception. Đã sửa import và dùng doThrow khi thay stub; không bỏ test,
  giảm assertion hoặc thay production để che lỗi test.

Reports XML/text ở `src/backend/target/surefire-reports/` và `failsafe-reports/`,
gitignored, không đưa output build/log vào commit. Full clean verify thay reports
của focused run; số liệu focused ở trên lấy từ lượt chạy thực tế trước đó.

## Review-fix — Missing TripStop không được làm mất verified LegFare

Phương xác nhận bug sau khi review source: policy dùng `StopFact.configured`
(sự tồn tại của TripStop) để quyết định giữ fare. Đã bỏ phụ thuộc này khỏi xác minh
fare; vẫn yêu cầu Route/Stop được xác minh, index không âm, adjacency đúng và giá
trong safe integer. Kiểm tra completeness TripStop, timestamp/null, Segment và
cộng tiền an toàn giữ nguyên. Không sửa repository, service hoặc schema.

Regression mới trong `TripDetailsPolicyTest`:

- Ba ca A–B/B–C/A–C với RouteStop A–B–C và đủ hai fare, TripStop B missing:
  `stops` chỉ A/C, giữ index 0/2 và cả hai LegFare; giá lần lượt 100000/150000/250000.
  Giờ xuống B/giờ lên B là null; A–C giữ đúng giờ có nguồn tại A/C. Eligibility false
  với `ESSENTIAL_DATA_INCOMPLETE`, không tạo TripStop B.
- Ba ca thiếu fare, fare vượt safe integer, fare sai adjacency khi B missing:
  không xuất leg không hợp lệ hoặc giá Segment chưa xác minh.
- Route chưa xác minh dù còn raw Stop/fare: không xuất fare hoặc selectedSegment.
  Các tests cũ về RouteStop gap ngoài Segment, zero/âm/overflow vẫn được giữ.

**Giới hạn fixture PostgreSQL đã kiểm chứng:** V3 có hai FK
`leg_fare_from_trip_stop_fk` và `leg_fare_to_trip_stop_fk` từ LegFare tới TripStop.
IT mới `v3PreventsDeletingTripStopWhileReferencedLegFaresRemain` thử xóa riêng B
trong transaction, không xóa fare: PostgreSQL từ chối với SQLSTATE **23503** và
tên FK tương ứng. Test rollback về savepoint, kiểm tra hai fare và TripStop B vẫn
còn. Không drop/sửa/defer constraints. Vì vậy trạng thái "B mất nhưng cả hai fare
còn" được kiểm chứng ở policy, **không tuyên bố tái hiện được dưới V3 hiện hành**.
IT cũ xóa fare trước rồi xóa B vẫn kiểm chứng partial details có thể tạo hợp lệ.

Focused review-fix dùng cùng lệnh verify chọn P4 ở trên: BUILD SUCCESS, exit 0,
**61 Surefire + 25 Failsafe = 86 tests**, 0 failures/errors/skipped; kết thúc
`2026-11-18T06:41:43Z` (13:41:43 VN). Đối chiếu XML: controller 11, parser 8,
service 4, policy 38, repository IT 24, snapshot IT 1.

Full review-fix chạy `docker compose -f compose.test.yaml run --rm backend-test`:
BUILD SUCCESS, exit 0, **284 Surefire + 264 Failsafe = 548 tests**, 0 failures/errors/skipped;
kết thúc `2026-11-18T06:44:08Z` (13:44:08 VN). Đã đối chiếu XML reports, gồm cả
tám regression cases mới (bảy policy, một IT kiểm tra FK). Sau đó chạy Compose
`down`, exit 0, không `-v`. Không chạy QA/UI hoặc nghiệm thu public P4 trong lượt này.
SHA256 so với đầu lượt review-fix xác nhận chỉ policy, hai file test nêu trên và
handoff này thay đổi; 15 file P4 còn lại giữ nguyên. Contract/migration/dependency
không đổi; không commit/push/merge/rebase/đổi branch.

## Checkpoint C2 — anonymous GET details

Phương xác nhận Trung Hiếu cho phép tự tích hợp public security P4 trong task này.
Thêm duy nhất matcher production trong `SecurityConfiguration.java`, ngay sau hai
matcher P2 và trước `anyRequest().authenticated()`:

```java
.requestMatchers(HttpMethod.GET, "/api/v1/trips/{tripId}").permitAll()
```

Matcher method-scoped GET, `{tripId}` là đúng một path segment; không dùng `/**`,
không mở nested routes hoặc writes. Matcher P2 giữ nguyên. Giữ CSRF/session/filter
chain, security error body và authorization cho các route còn lại. Không thêm JWT
hoặc sửa cấu hình khác.

File trong lượt C2:

- `identity/infrastructure/SecurityConfiguration.java`: một matcher GET P4.
- `trip/api/TripDetailsControllerTest.java`: đổi assertion anonymous 401 cũ thành
  anonymous 200/no-store, xác nhận service nhận request không chọn Segment.
- Mới `trip/api/TripDetailsSecurityMatchersTest.java`: test slice có controller probe
  chỉ trong test, dùng SecurityConfiguration thật; chứng minh routes tồn tại.
- Mới `trip/api/PublicTripDetailsSecurityIT.java`: HTTP thật qua random port và
  security filter tới PostgreSQL 16, không cookie/auth/test user cho public GET.
- Theo Phương cho phép mở rộng tối thiểu phạm vi: `SecurityConfigurationTests.java`
  và `PublicTripsSecurityIT.java` thay trường hợp details trong danh sách protected
  cũ bằng nested `/operations`; giữ nguyên mọi ca protected/nested khác và số tests.
  Positive public details được kiểm tra ở các tests P4, không bỏ coverage.
- File handoff này. Không sửa business logic Trip/P2/P3/Booking hoặc schema/fixture.

Negative security evidence được thiết kế để không dựa vào 404/405:

- HTTP POST/PUT/PATCH/DELETE details: thiếu/sai CSRF → 403 `ACCESS_DENIED`; có cookie
  session và CSRF token thật nhưng anonymous → 401 `UNAUTHENTICATED`.
- Test slice có write controller thật tại cùng path: authenticated + CSRF hợp lệ
  tới controller và trả 200; thiếu/sai CSRF bị 403, anonymous + CSRF hợp lệ vẫn 401.
- Tại checkpoint C2 riêng P4, nested `/seat-availability`, `/operations`, `/nested/deeper`: anonymous bị 401 từ
  filter; trong test slice, authenticated tới đúng controller probe và trả 200.
- HTTP kiểm tra collection trailing slash, details trailing slash, nested/deeper và
  protected routes khác bằng 401/`UNAUTHENTICATED`; P4 repository không được gọi.
  Test slice kiểm tra HEAD/OPTIONS không được public bởi matcher GET.
- GET details hợp lệ kiểm tra toàn bộ JSON/no-store từ fixture; committed fare update
  được phản ánh ở HTTP kế tiếp. Không chọn Segment → 200 với explicit null;
  sai UUID/unpaired/duplicate → 400, Trip mất → 404, Segment đảo → 422.
- Ca DB retrieval dùng spy chỉ để đặt `SET LOCAL search_path TO pg_catalog` trên
  transaction server, rồi chạy SQL repository thật: kiểm tra 503/no-store/body an toàn;
  request kế tiếp đọc lại được. Không mock dữ liệu/lỗi repository để thay bằng chứng DB.
- Cả tests P2 public catalogue/search và CSRF/protected regression được chạy cùng lượt.

Lệnh focused C2 từ gốc repo:

```powershell
docker compose -f compose.test.yaml run --rm backend-test sh ./mvnw -B -ntp verify '-Dtest=SecurityConfigurationTests,TripDetailsControllerTest,TripDetailsSecurityMatchersTest' '-Dit.test=PublicTripDetailsSecurityIT,PublicTripsSecurityIT,PublicStopsSecurityIT' -DfailIfNoTests=true
```

Focused C2: BUILD SUCCESS, exit 0, **41 Surefire + 37 Failsafe = 78 tests**,
0 failures/errors/skipped; kết thúc `2026-11-18T06:55:45Z` (13:55:45 VN).
Đối chiếu XML: SecurityConfigurationTests 19, TripDetailsControllerTest 11,
TripDetailsSecurityMatchersTest 11, PublicTripDetailsSecurityIT 23,
PublicTripsSecurityIT 9, PublicStopsSecurityIT 5. PostgreSQL 16.16, Java 21.0.12.1.

Full C2 dùng `docker compose -f compose.test.yaml run --rm backend-test` (clean verify):
BUILD SUCCESS, exit 0, **295 Surefire + 287 Failsafe = 582 tests**, 0 failures/errors/skipped;
kết thúc `2026-11-18T06:58:52Z` (13:58:52 VN). Đã cộng lại XML reports của lượt full;
hai lớp security mới vẫn 11 + 23 tests PASS. Đã chạy Compose `down`, exit 0, không `-v`.
`git diff --check` và whitespace cho các file mới/thay đổi đều đạt. SHA256 xác nhận
17 file P4 có sẵn khác giữ nguyên; lượt C2 chỉ thêm hai tests, sửa controller test,
handoff và ba file tracked đã liệt kê. Không commit/push/merge/rebase hoặc đổi branch.

## Còn chờ và giới hạn nghiệm thu

- Checkpoint C2 đã thay trạng thái anonymous 401 của lượt triển khai ban đầu bằng
  public GET details. PostgreSQL MockMvc IT cũ dùng authenticated test user chỉ là
  bằng chứng API/DB; bằng chứng anonymous HTTP hiện nằm ở PublicTripDetailsSecurityIT.
- Route/Stop inactive vẫn chờ Trung Hiếu confirmation như nhãn ở trên.
- Chưa chạy QA/UI, Q3 hoặc demo frontend; chưa có
  approval implementation từ reviewer. Maven xanh không chứng minh các phần đó đã đạt.
- Không commit/push/merge/rebase hoặc đổi nhánh; dừng chờ Phương review.


## Checkpoint ghép P4 + H2 — 18/11/2026

- Merge P4 `fb28ece3` (PR #13) vào nhánh Huy đang có H2 `47c21c6` (PR #14),
  base main `0115a8c`; giữ lịch sử tác giả, không sửa nhánh Phương.
- Giữ riêng hai matcher public GET details và seat-availability. Trong bốn lớp
  security, probe private dùng `/seat-availability/private` thay cho GET ghế;
  giữ assertion private, writes/authentication/CSRF. Không mở wildcard.
- Đã quan sát test cũ thất bại: `TripDetailsSecurityMatchersTest` đòi GET ghế 401
  nhưng nhận 200; sau sửa kỳ vọng, nhóm liên quan đạt **81/81** (34 unit, 47 IT).
- Full verify PostgreSQL 16/Java 21: **587/587 PASS** (299 unit, 288 integration),
  không failure/error/skip. Đây là kết quả bản ghép; 582/467 ở các checkpoint riêng
  không được dùng thay cho kiểm chứng này.

```bash
docker compose -p team11-huy-booking-test -f compose.test.yaml run --rm backend-test
docker compose -p team11-huy-booking-test -f compose.test.yaml down
```

HTTP thật trên DB demo tmpfs riêng đã PASS P4 → H2 cho A→B, A→C, B→C: eligibility,
giá 100.000/250.000/150.000, UUID Trip/Stop/layout và đủ sáu trạng thái ghế đúng seed.
P4 không chọn Segment trả selectedSegment null. Không đăng nhập, không mock port.
Private routes trả 401; POST/PUT/PATCH/DELETE trên cả hai path trả 403 khi thiếu/sai
CSRF và 401 khi có CSRF thật nhưng chưa xác thực. GET không đổi các Booking đã seed.
Containers demo/test đã dừng bằng đúng project, không `-v`; logs/request/response
lưu ngoài commit. Không lặp vòng expiry/cleanup/hết ghế đã chạy ở checkpoint H2
vì không đổi logic hoặc fixtures.

Hướng dẫn tiếp nhận nằm ở [README Booking](../../src/backend/src/main/java/vn/edu/team11/coach/booking/README.md),
CI/commit cuối tại PR #14. PR #13 và #14 có code chung sau lần ghép này; Trung Hiếu
điều phối merge, không cần merge lặp phần P4. Runtime QA bổ sung của Trip, nghiệm thu
frontend và xác nhận chính thức cho Route/Stop inactive vẫn là đầu việc riêng;
checkpoint này không thay approval nghiệp vụ hoặc báo cáo QA tổng hợp.
