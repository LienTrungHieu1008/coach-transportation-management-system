# Kiểm chứng H1 — core logic ghế

Ngày 15/11/2026. Branch `feat/h1-seat-allocation`, base main `0760fba871fc4be98210b82f8e208d88cc40ac6c`.

Phạm vi theo [kế hoạch đã được người dùng giao](../superpowers/plans/2026-11-15-h1-seat-allocation.md), [TASKS](../../pa/PA4/TASKS.md) và [C1](../../pa/PA4/C1_DECISIONS.md). **Đã có core logic/tests; chưa hoàn thành toàn bộ H1 hoặc UC-P07.**

## Thay đổi và truy vết

Code tại `vn.edu.team11.coach.booking.domain`, không phụ thuộc Spring/JPA hoặc module khác:

- `Segment`: chỉ số `0 <= from < to`, khoảng nửa mở và `overlaps` — SAD 1.3 mục 4.4 C1/C2, BR-P07-02, TC-13.
- `BookingStatus` và `SeatAllocation`: dữ liệu đọc immutable, kind Hold/Confirmed, `isEffective(asOf)` xét Booking status/releasedAt/expiry — SAD C3/D6, BR-P07-01/02, TC-13/14.
- `SeatAllocationService`: nhận Trip/Segment, danh sách physical seat ID theo layout đã xác minh, allocations và một Instant asOf. Trả `SeatState` theo đúng thứ tự, đầy đủ, immutable; FREE/null hoặc UNAVAILABLE với expiry thích hợp theo OpenAPI 0.1.

SeatAllocation chứa Booking status để đánh giá allocation trong phép đọc; chưa là entity hoặc quyết định mapping V4. Kiểu kết quả chỉ có FREE/UNAVAILABLE cho facts đã xác minh; không thay thế DTO/enum HTTP có UNKNOWN trong OpenAPI.

Không tạo Clock/configuration mới: thời điểm có thẩm quyền được caller truyền vào. Không tạo ID, dữ liệu mẫu hoặc log trong production. Không đổi dependency, migration, API/port, security, CI, Identity, Trip hoặc frontend.

## Luồng xử lý và một nhánh lỗi

`getAvailability` kiểm tra input → lọc đúng Trip/ghế/Segment và allocations hiệu lực → nhóm blocker theo physicalSeatId → trả trạng thái theo thứ tự ghế đầu vào. Confirmed chặn thì expiresAt=null; chỉ Hold chặn thì chọn expiry sớm nhất, không hứa ghế sẽ FREE ở mốc đó.

Ví dụ cùng Trip/ghế: Confirmed A–B không chặn truy vấn B–C nhưng chặn A–C. Với Pending/Hold, tại đúng expiresAt ghế không còn bị hold đó chặn, dù Booking vẫn Pending; core không chuyển trạng thái hoặc gia hạn hold.

Segment âm/bằng/đảo chiều, layout rỗng hoặc ID ghế trùng bị IllegalArgumentException; dữ liệu bắt buộc null, kể cả expiry của Hold, bị NullPointerException. Không tự sửa input hoặc chuyển lỗi thành FREE. Chưa có ánh xạ lỗi HTTP; caller tương lai phải phân biệt lỗi input và facts không xác minh được theo contract.

## Kiểm thử đã chạy

Môi trường thực tế: Docker Compose project riêng `team11-huy-h1-test`, Java 21.0.12.1, Maven Wrapper/Maven 3.10.0, PostgreSQL **16.16**. DB tmpfs, không công bố port, tách volume phát triển. Không H2 hoặc skip tests.

Baseline và final từ gốc repo:

```bash
docker compose -p team11-huy-h1-test -f compose.test.yaml run --rm backend-test
docker compose -p team11-huy-h1-test -f compose.test.yaml down
```

Container chạy `clean verify -DfailIfNoTests=true`. Sau baseline đã down project để final chạy trên DB tmpfs mới; sau final đã down thành công, giữ cache Maven và output kiểm chứng.

| Lượt kiểm tra | Kết quả thực tế |
|---|---|
| Baseline trước H1 | Exit 0; 5 unit + 38 PostgreSQL IT = 43 tests, không failure/error/skip. |
| RED Segment | Viết tests trước; test compilation thất bại vì chưa có Segment. Sau implementation: 13/13 đạt. |
| RED allocation | Viết tests trước; test compilation thất bại vì chưa có BookingStatus/SeatAllocation. Sau implementation: 42/42 domain tests đạt. |
| RED service | Viết tests trước; test compilation thất bại vì chưa có SeatAllocationService và kiểu kết quả. Sau implementation: 63/63 domain tests đạt. |
| Final full backend verify | Exit 0, BUILD SUCCESS; 68 unit + 38 PostgreSQL IT = **106 tests**, failures/errors/skipped đều 0; đối chiếu XML Surefire/Failsafe. |

Các lượt targeted dùng cùng Compose project/image/cache và lệnh:

```bash
docker compose -p team11-huy-h1-test -f compose.test.yaml run --rm backend-test \
  sh ./mvnw -B -ntp -Dtest=SegmentTests,SeatAllocationTests,SeatAllocationServiceTests \
  test -DfailIfNoTests=true
```

Ở từng bước chỉ liệt kê các test class đã được thêm. RED ban đầu là lỗi thiếu production types khi biên dịch, không phải assertion failures; không tuyên bố đã chạy test bodies ở các lượt đó.

| H1 test class | Invocations | Hành vi kiểm tra |
|---|---:|---|
| SegmentTests | 13 | Invalid indices, đoạn kề/rời/trùng/chứa/giao nhau, đối xứng, biên int và peer null. |
| SeatAllocationTests | 29 | Toàn bộ Booking/kind/released combinations, trước/đúng/sau expiry tới nanosecond, Confirmed không hết hạn theo hold cũ, required input/asOf. |
| SeatAllocationServiceTests | 21 | Đúng Trip/ghế/Segment, hết ghế, earliest expiry và Confirmed priority ở hai thứ tự input, expired/cancelled/released/mismatch, refresh không ghi/gia hạn, danh sách immutable/không sửa input, invalid query/layout/entries. |

Logs và bản sao XML baseline/domain nằm trong `tmp/h1-seat-allocation/` bị gitignore; XML final nằm trong `src/backend/target/{surefire-reports,failsafe-reports}`. Cảnh báo metadata repository Flyway 401 và Mockito dynamic agent đã xuất hiện cả baseline/final; không đổi cấu hình để tắt hoặc che cảnh báo. Generated account warning thuộc SecurityConfigurationTests slice như baseline, không thêm account đăng nhập vào runtime.

## Review

Review độc lập bằng agent chỉ đọc đã rà đủ bốn production types, ba test classes, kế hoạch, báo cáo và contract nguồn; không phát hiện Critical/Important/Minor. Reviewer đối chiếu log/XML 106 tests, không chạy lại Maven hoặc sửa checkout. Những phần V4/H2/H3/P08/P09 được giữ ngoài phạm vi; releasedAt được hiểu theo snapshot hiện tại (`null` là chưa released), chưa cam kết truy vấn lịch sử. Kết quả này không thay review/approval của Minh Phương hoặc quyền merge của leader.

## Điểm bàn giao và giới hạn

- Minh Phương review core H1. Review bằng agent không thay approval của thành viên hoặc quyền merge của leader.
- Chờ V3, implementation TripQueryPort và manifest Trip/Stop/layout để chốt V4/FK/fixture và kiểm chứng H1 persistence/H2 với DB thật. Không copy PR #3 hoặc tạo lại dữ liệu Trip.
- H2 chưa có HTTP endpoint, security/error mapping hoặc snapshot liên module. H3 chưa có job hoặc Pending→Expired; người dùng đã chọn làm phần đó ở đợt H3.
- QA có thể dùng các ca trên làm đầu vào expected; 63 domain tests không thay functional report 5 ca P07 hoặc tổng 15 ca PA4. 38 IT hiện tại chỉ kiểm chứng nền tảng/Account, không chứng minh Booking persistence.
- Nội dung SAD PA4 cần phản ánh bốn kiểu domain này, caller-supplied asOf, phép đọc không ghi và các dependency còn thiếu; không sửa PDF PA3 đã nộp. Chưa commit/push/mở PR/merge và chưa kiểm tra CI cho thay đổi H1 này.

## Checkpoint phụ thuộc ngày 16/11/2026

Controller đã `git fetch --prune origin` thành công: `origin/main` vẫn là `0760fba871fc4be98210b82f8e208d88cc40ac6c`, chỉ có V1/V2, chưa có V3 hoặc package Trip production. PR #4 (V3) đang open/Draft, `merged_at=null`, head `2b671b95111985084bd57880cd86df23e65fc13f`; PR #3 đang open/Draft, `merged_at=null`, head `f4f7e3cc7e398d86c02737efac9aed8dccd2b8c0`, chỉ interface/DTO, chưa có adapter thật. Đây là trạng thái tại checkpoint, không sửa kết quả lịch sử phía trên.

Người dùng chọn chỉ dùng dependency đã merge vào `main`, không stacked branch/copy/cherry-pick. Vì vậy chưa bắt đầu phần feature tiếp theo: H1/V4 chờ V3/manifest trên main; H3 chờ H1/V4 trên main; H2 chờ H1 và contract port trên main, còn nghiệm thu runtime phải có adapter thật cùng DataSource/snapshot và phần public GET/security/error chung của leader. Mock unit/MVC không thay nghiệm thu runtime.

Đã lưu [kế hoạch tiếp tục Booking](../superpowers/plans/2026-11-16-booking-continuation.md) theo phương thức Subagent-driven người dùng chọn. Checkpoint này chỉ cập nhật tài liệu, không sửa source/migration/test hoặc chạy lại backend tests; H1 domain và output cũ được giữ nguyên. Không commit/push/mở PR/merge. H1 vẫn hoàn thành một phần; H2/H3 chưa hiện thực.

## Chuyển đích PR sang P1 — 16/11/2026

Người dùng sau đó chọn giữ `feat/h1-seat-allocation`, mở PR vào `feat/p1-trip-schema` và chỉ chuyển sang code trực tiếp trên nhánh Phương sau khi PR H1 được ghép. Lựa chọn này thay thế điều kiện chỉ dùng dependency trên main ở checkpoint trước; không cấp quyền tự merge.

Đã fetch và fast-forward nhánh Huy lên P1 `2b671b95111985084bd57880cd86df23e65fc13f`. Đối chiếu SHA-256 xác nhận cả 11 file local được giữ nguyên sau fast-forward; chỉ cập nhật tài liệu workflow/bằng chứng sau đó. Bốn production types và ba test classes H1 không đổi. Không copy/cherry-pick Trip hoặc sửa V3 đã chia sẻ.

Đã chạy lại trên working tree H1 + P1, Java 21 và PostgreSQL 16 test riêng:

```bash
docker compose -p team11-huy-booking-test -f compose.test.yaml run --rm backend-test
docker compose -p team11-huy-booking-test -f compose.test.yaml down
```

Container chạy `clean verify -DfailIfNoTests=true`: **BUILD SUCCESS**, exit 0; **130 tests** gồm 68 unit (63 H1 + 5 security) và 62 integration (35 Account + 3 Foundation + 24 Trip). Không failure/error/skip. Cleanup exit 0; không xóa volume phát triển. XML tại `src/backend/target/{surefire-reports,failsafe-reports}`; log và tổng hợp tại `/tmp/team11-huy-branch-transfer-2m78okhq/`. Frontend không đổi, không chạy lại FE checks.

Review agent đọc đủ 11 file và đối chiếu UC/SAD, log/XML, không phát hiện lỗi core hoặc chất lượng code; không thay review của Minh Phương/leader. Workflow CI hiện chỉ chạy PR vào `main`, nên không suy diễn PR có base P1 đã được GitHub CI kiểm chứng.

Người dùng đã giao commit/push/mở PR cho phạm vi core này. H1 vẫn chỉ có core logic/tests: chưa có V4/FK/Booking fixtures/persistence. H2/H3 chưa hiện thực. Bước tiếp theo sau khi PR core được ghép là V4 trên nhánh tích hợp P1; H2 còn cần contract/adapter Trip thật cùng snapshot và phần public route/error của leader theo kế hoạch tiếp tục.

Fetch cuối trước khi công bố cho thấy `origin/main` đã tiến tới `dd1ca72` sau khi PR #4 và #3 được merge. Nhánh đích P1 vẫn ở `2b671b9`; PR H1 tiếp tục dùng đích này theo lựa chọn người dùng. Kết quả 130 tests phía trên thuộc nền P1, không phải lượt verify trên main mới.
