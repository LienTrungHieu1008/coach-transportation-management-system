# P3 — bàn giao sửa review cho Phương/Huy, 17/11/2026

Phạm vi leader giao: hai điểm review của PR #6 trên `feat/p3-trip-postgres-adapter`, tiếp nối commit gốc của Phương `96623d7`. Giữ nguyên lịch sử; không merge. Không thay migration V1–V3, DTO/OpenAPI/port, dependency, security hoặc CI.

## 1. Lỗi production đã sửa

`PostgresTripQueryAdapter.getSeatAvailabilityFacts` trước đây lấy `tripStops.subList(from,to+1)` để kiểm tra giá. Điều này bỏ qua giá thiếu ngoài Segment. Vòng kiểm tra nay duyệt mọi cặp TripStop liên tiếp đã xác minh trên toàn chuyến. Thiếu giá hoặc giá âm vẫn trả `ESSENTIAL_DATA_INCOMPLETE`; lỗi truy xuất vẫn map `TRIP_DATA_UNAVAILABLE`. Không thay giá thiếu bằng 0 và không thay Segment được trả về.

Hai test mới trong `PostgresTripQueryAdapterIT`:

- `missingFareOutsideSelectedSegmentReturnsEssentialDataIncomplete`: xóa B–C, hỏi A–B; phải bị từ chối. Đã chạy RED trên adapter cũ: không có exception như kỳ vọng.
- `completeTripFaresAllowShortSegment`: giữ đủ A–B/B–C; hỏi A–B vẫn trả đúng indices 0/1 và 6 ghế.

## 2. Bổ sung bằng chứng transaction, không kết luận sai implementation cũ

Giữ nguyên annotation `MANDATORY` của adapter. Caller vẫn chịu trách nhiệm mở read-only `REPEATABLE_READ` theo contract.

`PostgresTripQuerySnapshotIT` nạp fixture đã commit trong PostgreSQL test riêng. Reader gọi port và thấy ghế A1; sau đó CountDownLatch cho writer ở thread/connection khác đổi nhãn thành A1-updated. `Future.get` chờ transaction writer commit thành công; kiểm tra `pg_backend_pid()` khác nhau. Reader trong transaction cũ vẫn thấy A1; transaction mới thấy A1-updated. Không dùng sleep.

Latch/future có timeout, lỗi worker được truyền về test; executor được đóng trước cleanup. `@Sql` AFTER_TEST_METHOD dọn đúng fixture theo FK order bằng `trip_snapshot_cleanup.sql`, kể cả test thất bại; không xóa schema/DB dùng chung hoặc dùng CASCADE.

Đã chạy negative control: chỉ đổi isolation của **test caller** sang READ_COMMITTED, test thất bại đúng ở lần đọc sau commit (`expected A1, actual A1-updated`). Đã phục hồi REPEATABLE_READ trước full verify. Negative control không thay transaction production và không được commit.

## 3. Phương theo dõi và đọc lại code

Trong PR #6, mở **Commits** để xem riêng commit sửa giá và commit bằng chứng snapshot; **Files changed** xem tổng diff. Hai commit tiếp nối code của Phương, không squash/rebase/force-push lịch sử khi bàn giao.

```powershell
git fetch origin
git switch feat/p3-trip-postgres-adapter
git pull --ff-only origin feat/p3-trip-postgres-adapter
git log --oneline 96623d7..origin/feat/p3-trip-postgres-adapter
git show <ma-commit-can-doc>
git diff 96623d7..origin/feat/p3-trip-postgres-adapter -- src/backend/src/main/java/vn/edu/team11/coach/trip/infrastructure/PostgresTripQueryAdapter.java
```

Nếu còn sửa local, giữ nguyên và thống nhất cách ghép với leader; không reset/force-push để lấy bản mới. Thứ tự đọc: regression test → vòng kiểm tra giá → snapshot test → fixture cleanup.

Huy rà: lỗi thiếu leg ngoài Segment, ca đủ giá không bị từ chối, reader/writer dùng connection khác, chờ commit thật, cleanup và scope thay đổi. Phương cần giải thích được lý do duyệt toàn chuyến và vì sao MANDATORY không tự đặt REPEATABLE_READ.

## 4. Kiểm tra và giới hạn

Lệnh full từ gốc repo: `docker compose -f compose.test.yaml run --rm backend-test`, thực thi Maven Wrapper `clean verify -DfailIfNoTests=true` trên Java 21/PostgreSQL 16 test riêng; sau đó `docker compose -f compose.test.yaml down`.

Kiểm tra local đã thực hiện:

- Full verify trước sửa production: 12 unit tests đạt; 77 integration tests có đúng 1 failure tại ca thiếu giá ngoài Segment (không có exception); không error/skip. Ca đủ giá A–B đã đạt.
- Negative control snapshot: 12 unit tests đạt; snapshot IT thất bại đúng tại assertion sau commit (`expected A1, actual A1-updated`), không error/skip. Lần gọi Maven đầu bị PowerShell tách tham số `-Dit.test`; đã quote tham số và chạy lại, không tính lỗi CLI là bằng chứng test.
- Đã phục hồi REPEATABLE_READ trong source. Lượt full verify cuối local bị chặn trước khi bắt đầu bởi Docker engine ngừng phản hồi; `docker info` timeout 25 giây. Không ghi local GREEN từ các XML còn lại của lượt chạy trước, vì chúng trộn RED và negative control.
- GitHub CI trên đúng head mới của PR #6 là nơi xác minh full backend verify sau sửa; xem checks của PR để biết kết quả. Không chạy FE local vì không đổi FE; CI vẫn chạy FE theo workflow hiện có.

Không dùng negative control thất bại làm kết quả final và không giảm checks để xử lý môi trường. Test Compose là project riêng với DB tmpfs; cleanup cuối phụ thuộc Docker phản hồi, phải kiểm tra lại nếu engine còn treo.

Snapshot test chỉ kiểm chứng adapter Trip trong caller transaction. Chưa chứng minh snapshot xuyên Trip–Booking hoặc flow H2; phần đó tiếp tục phối hợp Huy khi implementation H2 sẵn sàng. Không coi agent verification là approval của Phương/Huy.
