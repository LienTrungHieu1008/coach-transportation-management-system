# C1 — quyết định của leader ngày 14/11/2026

**Trạng thái: Trung Hiếu đã đồng ý toàn bộ phương án ngày 14/11/2026**, gồm contract API/port, quy tắc biên, migration V2 Account → V3 Trip → V4 Booking và dữ liệu demo. Đây là quyết định kỹ thuật của leader để giao việc; chưa ghi là các thành viên khác đã nhận task hoặc approve. Leader thông báo nhóm; người triển khai báo ngay nếu phát hiện mâu thuẫn SAD/UC hoặc không đáp ứng được, không tự đổi contract.

## 1. Baseline và phạm vi

- Đã `git fetch origin` ngày 14/11: `origin/main` ở `f3895f6`, merge PR #1 và chứa contract `bf2a3ae`. Phần này đã xác minh; chưa kiểm tra thêm trạng thái approval/settings trên GitHub.
- Giữ P04 tìm chuyến, P05 chi tiết, P07 ghế trống. Bốn GET `/api/v1/stops`, `/api/v1/trips`, `/api/v1/trips/{tripId}`, `/api/v1/trips/{tripId}/seat-availability`.
- Dùng [OpenAPI 0.1](../../docs/contracts/openapi.yaml) và [TripQueryPort](../../docs/contracts/trip-query-port.md) làm contract giao việc đã được leader duyệt tại C1. Không thêm login, tạo Booking, thanh toán hoặc AI vào luồng demo.

## 2. Quy tắc và response đã chốt

| Điểm | Quyết định của leader |
|---|---|
| Dữ liệu chung | UUID string, camelCase; thời điểm UTC kết thúc Z; hiển thị/lọc ngày theo Asia/Ho_Chi_Minh. Giá VND integer, BE tính tổng leg, không FE tự cộng tiền. |
| Search | Chỉ Scheduled, `now < bookingClosesAt`; ngày tìm là ngày tại Stop lên. Sort giờ lên rồi tripId; page=0, size=20, tối đa 100. Không khớp: 200/items rỗng. |
| Cutoff | `bookingClosesAt = giờ rời Stop đầu - 30 phút`; đúng mốc là đóng. Không đổi cutoff theo Stop giữa tuyến. |
| Details | Cặp Stop optional cùng nhau; không chọn thì selectedSegment=null. Trip đóng hoặc thiếu essential: vẫn 200 với thông tin xác minh được, eligibility=false và lý do. Missing optional description không chặn. |
| Search có dữ liệu hỏng | Candidate phù hợp nhưng thiếu cấu hình bắt buộc: 503, không bịa hoặc âm thầm loại. Vì vậy fixture dữ liệu lỗi được nạp theo kịch bản riêng, không đặt trong bộ demo tìm chuyến bình thường. |
| Ghế | FREE/UNAVAILABLE/UNKNOWN. Truy xuất không xác minh được: 503; FE hiển thị UNKNOWN, không tự xem ghế là trống. Ghế hết: 200 với toàn bộ ghế UNAVAILABLE. |
| Hold | Hiệu lực khi Pending/Hold chưa released và now < expiresAt; đúng expiry không chặn. Confirmed chặn khi Booking/Allocation cùng Confirmed. Đoạn kề nhau không giao nhau. |
| Refresh và cleanup | FE refresh mỗi 15 giây khi tab hiển thị, khi foreground và khi tới expiry/cutoff; không tự chuyển FREE. Booking job cleanup chu kỳ tối đa 5 giây khi hoạt động bình thường, Pending hết hạn chuyển Expired. Query luôn loại hold hết hạn dù job trễ. |
| Layout | deck/row/column 0-based như OpenAPI; physicalSeatId là định danh, label là nhãn hiển thị; không suy ID từ vị trí. |
| Lỗi/cache | 400 format/cấu trúc; 422 nghiệp vụ; 404 Trip mất; 409 P07 không thể tiếp tục; 503 không xác minh được dữ liệu; 500 lỗi nội bộ. Body code/message/fieldErrors. Tất cả response khai báo no-store. |

Request/response/schema chính xác và ví dụ nằm trong OpenAPI; không tạo một bộ DTO khác trong tài liệu này.

## 3. Điểm ghép và ai sửa gì

- **Phương:** Trip/Stop/leg fares/Vehicle/PhysicalSeat/layout; ba API Trip và `TripQueryPort.getSeatAvailabilityFacts(tripId, fromStopId, toStopId, asOf)`.
- **Huy:** Booking/BookingSeat/SeatAllocation và expiry job. Nhận Trip ID, Stop indices, cutoff, layout qua port rồi tính trạng thái ghế. Không đọc repository Trip.
- **Hoàng:** bốn màn và client API chung; truyền nguyên tripId/fromStopId/toStopId từ search → details → map. Không tự tính giá, cutoff hay availability.
- **Trung Hiếu:** schema Account prerequisite bên dưới, điều phối migration, thay đổi chung security/error mapping và tích hợp. Chỉ mở GET đúng contract khi endpoint được triển khai; không mở `/api/**` toàn bộ.
- Cách phối hợp C2: owner API gửi method/path và lỗi cho Hiếu ngay khi endpoint sẵn sàng để ghép phần chung trong luồng PR thống nhất, không chờ đủ cả ba UC. Phương/Huy vẫn viết điều kiện lỗi nghiệp vụ/tests và sửa bug module mình; Hiếu không viết lại các module. Owner và bàn giao chi tiết tại mục Phối hợp làm việc trong [TASKS](TASKS.md).
- **Đức Hiếu:** QA/ma trận ca và review chéo; mỗi chủ module tự viết tests, tài liệu phần mình và sửa bug của mình.
- P07 dùng cùng server asOf và snapshot read-only cho Trip/layout/allocations theo port (REPEATABLE READ, REQUIRED cùng DataSource); không khóa/giữ ghế trong GET. Đây là snapshot thông tin, không cam kết ghế cho lần đặt sau.

## 4. Migration đã phân công — I1/V2 đã viết và test

Lúc chốt C1 repo mới có V1 session; I1 sau đó đã thêm V2 và verify trên PostgreSQL test, còn chờ review/merge. Xem [contract Account](../../docs/contracts/identity-account.md) và [kiểm chứng I1](../../docs/test/i1-account-verification.md). SAD mô tả Booking có passengerAccountId và liên hệ Account; không nên xóa ownership hoặc dùng passenger ID tùy ý chỉ để nạp fixture.

| Version giữ chỗ | Owner | Nội dung |
|---|---|---|
| V1 hiện có | Giữ nguyên | Spring Session; đã áp dụng, không sửa. |
| V2__identity_account.sql | Trung Hiếu; Hoàng review | Schema Account/role enum tối thiểu theo SAD và các ràng buộc cần cho Booking; không tạo login API hay account mặc định có thể đăng nhập. |
| V3__trip_read_model.sql | Phương; Huy review | Schema Trip/Stop/Route/xe/ghế/leg fares cần cho ba UC; theo SAD, không tạo bản dữ liệu Trip có thẩm quyền thứ hai. |
| V4__booking_availability.sql | Huy; Phương review | Booking/BookingSeat/SeatAllocation tối thiểu theo SAD; FK và vòng đời cần cho P07, không implement P08/P09. |

Leader giữ chỗ V2/V3/V4 theo bảng trên; trước khi chia sẻ SQL vẫn kiểm tra main và các PR đang mở không có version trùng. Nếu phát hiện đã có migration chia sẻ/áp dụng, báo leader cấp version mới, không đánh số lại file của người khác. Merge/áp dụng theo V2 → V3 → V4; không áp dụng V3 vào DB dùng chung trước V2 rồi bật out-of-order/skip để chữa. Các migration tiếp theo xin số mới; không sửa file đã chia sẻ/áp dụng. Không nạp account/demo records trong migration schema.

V2 là prerequisite dữ liệu đã được leader duyệt cùng C1 và giao Trung Hiếu tại task I1. Thiết kế bảng/cột/constraints cụ thể vẫn được owner đối chiếu SAD và review trong task schema; approval C1 không thay review DDL chưa được viết.

## 5. Một bộ dữ liệu demo dùng chung

**Phương giữ manifest Trip/Stop/seat UUID; Huy tham chiếu lại, không tự tạo bộ ID khác.** Dữ liệu hoàn toàn tổng hợp, không dữ liệu khách hàng. Không dùng JSON response examples làm dữ liệu trả cứng từ controller.

- Một Route A → B → C, hai leg fare A–B=100.000đ, B–C=150.000đ; giá A–C=250.000đ; xe demo tối thiểu sáu ghế có layout xác định.
- Trip Scheduled ban ngày: A 09:00, B 11:00, C 13:00 vào ngày D. Trip qua đêm: A ngày D 22:30, B ngày D+1 00:30, C ngày D+1 02:30. D mặc định là ngày mai ở Việt Nam tại lúc seed; giờ UTC được chuyển đúng khi ghi DB.
- Fixture riêng cho Cancelled/Departed/Completed và Scheduled đã đóng bán; không đưa các trạng thái đó vào search như chuyến còn bán. Trip đúng cutoff kiểm thử bằng Clock cố định.
- Ghế 1 FREE; ghế 2 Confirmed A–B (trống trên B–C); ghế 3 Confirmed A–C (bận trên B–C); ghế 4 có hold B–C; ghế 5 hold đã hết hạn; ghế 6 allocation cancelled/released không chặn. Records phải nhất quán theo các trạng thái Booking/Allocation trong SAD.
- Hold đang xem được tạo fixture với createdAt=seedTime−8 phút, expiresAt=seedTime+2 phút: vẫn đúng vòng đời 10 phút, không sửa rule thành hold 2 phút để demo.
- Bộ `all-seats-unavailable` và `incomplete-trip` nạp theo kịch bản riêng. Lỗi retrieval được kiểm chứng qua test lỗi hạ tầng có kiểm soát, không thêm switch giả lỗi vào API public.
- Account giả phục vụ FK là dữ liệu demo/test, không có login mặc định; không tạo Ticket/Payment để giả đã có P08/P09. Seed/reset chỉ cho DB local/test được chỉ định, không xóa dữ liệu thật hoặc DB dùng chung tùy tiện.
- Owner bàn giao cách seed/reset và input/expected cho từng kịch bản; tests dùng clock cố định, không phụ thuộc ngày hệ thống. Demo runtime dùng clock thật, seed mới khi ngày/hold đã cũ.

## 6. Bàn giao sau quyết định

1. Leader đã duyệt contract 0.1 tại C1 ngày 14/11. Phương/Huy/Hoàng rà để hiểu và báo mâu thuẫn nếu có, không cần chờ thêm một vòng duyệt cấu trúc trước khi chuẩn bị task. Chưa ghi họ đã approve nếu chưa phản hồi.
2. Leader gửi [TASKS](TASKS.md) và [handbook](../../docs/team-handbook.md) cho nhóm. Trung Hiếu bắt đầu I1; Phương P1/P3, Huy H1, Hoàng F1/F2, Đức Hiếu Q1. Trong lúc chờ V2, chuẩn bị code/tests; schema tích hợp phải theo thứ tự dependency.
3. Mốc trong TASKS là mục tiêu, chưa coi đã đạt: 14/11 bắt đầu prerequisite/schema, 15/11 ưu tiên port/data, 16–17/11 ghép API/UI, 18–19/11 thực thi/retest, 20/11 rà gói, 21/11 nộp. Mỗi owner xác nhận lịch thực tế.
4. TA xác nhận cách hiểu UC chính và mẫu Moodle/feedback PA3 vẫn là đầu vào riêng chưa có bằng chứng trong phiên; không tự đánh dấu đã hoàn tất. Các phần đó không ngăn nhóm chuẩn bị contract/schema/prototype/test cases đúng baseline đã chọn.

Đầu ra C1 kỹ thuật đã được leader duyệt. Thông báo/nhận việc của thành viên, TA và các đầu vào ở mục 6.4 vẫn chưa có xác nhận. Tài liệu quyết định không chứng minh API runtime, SQL migration hoặc tests nghiệp vụ đã được thực hiện.
