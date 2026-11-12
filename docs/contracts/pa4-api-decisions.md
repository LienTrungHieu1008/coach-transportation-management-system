# Trạng thái và quyết định API PA4 — 12/11/2026

## Đã được người giao việc đồng ý

Phạm vi P04/P05/P07 và cấu trúc **bốn GET công khai**: danh mục Stop, tìm chuyến, chi tiết chuyến, ghế trống theo Segment. Giữ REST `/api/v1`, tính giá/cutoff/availability ở backend; trang ghế không tạo hold. Quyết định được xác nhận trong phiên sau khi người dùng đọc tóm tắt đề xuất bốn endpoint.

[OpenAPI 0.1](openapi.yaml) cụ thể hóa request, response, enum, nullable và lỗi; [port Trip](trip-query-port.md) cụ thể hóa điểm ghép liên module. Đây là **bản contract để review trường dữ liệu**, chưa có xác nhận của Phương/Huy/Hoàng cho mọi chi tiết và chưa có implementation. Không diễn giải việc duyệt cấu trúc bốn API thành việc duyệt tất cả lựa chọn bên dưới.

## Quy tắc từ SAD/UC phải giữ

- P04 công khai, chỉ Scheduled còn mở bán, ngày theo Stop lên trong `Asia/Ho_Chi_Minh`; cutoff chung từ giờ khởi hành Stop đầu tuyến trừ 30 phút.
- Giá theo tổng leg liên tiếp; UUID/camelCase, VND integer, thời điểm UTC. P05 vẫn cho xem thông tin xác minh được khi đóng bán/thiếu cấu hình và chặn bước đặt; không đoán giá/giờ.
- P07 dùng Segment nửa mở; ghế bị chặn bởi Confirmed hoặc hold Pending còn hiệu lực trên Segment giao nhau; hết hạn/cancelled/released không chặn. Không lộ Booking hoặc dữ liệu cá nhân.
- Khi hold hết hạn, bản đồ phải cập nhật và Pending phải chuyển Expired; cleanup có thể trễ nhưng việc tính availability luôn xét expiresAt thật theo SAD D6.
- Không gọi repository/entity của module khác; ownership Vehicle/PhysicalSeat/layout ở Trip, allocations/Booking ở Booking.

Nguồn cụ thể tại [bản đối chiếu](pa4-api-proposal.md). Không sửa SAD/UC gốc để hợp thức hóa implementation.

## Lựa chọn cụ thể trong bản 0.1 — cần reviewer xác nhận

| Mục | Lựa chọn đề xuất để có contract kiểm tra được | Người rà |
|---|---|---|
| Cutoff đúng mốc | `serverTime >= bookingClosesAt` là đóng. Hold hết hiệu lực khi `serverTime >= expiresAt`. | Chủ UC/leader, QA |
| Input và lỗi | 400 cấu trúc/format/paging/duplicate scalar; 422 nghiệp vụ (SAD mapping); 404 Trip không tồn tại; 409 P07 không thể tiếp tục; 503 không xác minh được records. | Phương, Huy, Hoàng |
| Search thiếu dữ liệu | Candidate Scheduled phù hợp bị thiếu cấu hình bắt buộc: fail 503 `TRIP_DATA_UNAVAILABLE`, không âm thầm loại hoặc trả dữ liệu giả. Đây là lựa chọn bảo thủ cho P04 A3, cần Phương xác nhận. | Phương, QA |
| Cặp Stop cho P05 | Cả hai optional cùng lúc. Không có: `selectedSegment:null`. Có nhưng sai: 422. Thiếu cấu hình khiến không xác minh được: 200 với segment null/eligibility false; lỗi truy xuất thực sự: 503. | Phương, Hoàng |
| Ghế UNKNOWN | Giữ enum của SAD; server v0.1 trả 503 nếu không xác minh đủ bản đồ. FE chuyển trạng thái hiển thị sang UNKNOWN sau lỗi refresh, không coi stale map là dữ liệu mới. | Huy, Hoàng |
| Layout | `deck/row/column` integer bắt đầu 0; deck từ dưới lên, row từ đầu xe ra sau; cột từ trái qua phải khi nhìn về đầu xe, chừa khoảng số cho lối đi. Label là chuỗi nhà xe quản lý. Không suy ra seat ID từ tọa độ. | Phương, Huy, Hoàng |
| Refresh | Poll 15 giây khi tab hiển thị; refresh khi foreground hoặc đến expiry/cutoff. Một request mới nhất được quyền cập nhật UI; không để response cũ ghi đè response mới. Không tự chuyển FREE từ countdown. | Huy, Hoàng |
| Expiry cleanup | Booking job idempotent, đề xuất chu kỳ tối đa 5 giây; với scheduler/DB hoạt động bình thường, đánh dấu Expired trước lần cleanup thành công kế tiếp. Khi dịch vụ lỗi, không hứa thời gian hoàn tất nhưng availability vẫn loại hold hết hạn. Test dùng clock kiểm soát và gọi job trực tiếp, không sleep 5 giây để đoán kết quả. | Huy, QA |
| Paging/danh mục | Stop catalogue chưa paging; search page 0, size 20, tối đa 100, sort boardingTime/tripId; page vượt cuối trả items rỗng nhưng tổng thật. Số nguyên JSON không vượt safe integer; không thêm dependency để đổi stack. | Phương, Hoàng |
| Cache/error body | GET trả no-store; `{code,message,fieldErrors:[{field,message}]}`, chưa bắt buộc traceId vì baseline chưa tracing. Không sửa error body security của baseline trong task contract. | Phương, Huy, Hoàng |

Không còn câu hỏi placeholder trong schema; các lựa chọn đã ghi rõ để reviewer có thể đồng ý hoặc yêu cầu thay đổi cụ thể. Đổi các lựa chọn này phải cập nhật OpenAPI, examples, port và FE/BE/test liên quan trước khi các bên dùng.

## Bàn giao để code riêng

1. Phương/Huy/Hoàng review bảng và schema, leader chốt phiên bản contract được dùng chung. Không bắt đầu code dựa trên mỗi người tự sửa một DTO khác nhau.
2. Phương nhận `listStops/searchTrips/getTripDetails` và cung cấp `TripQueryPort.getSeatAvailabilityFacts`; Huy nhận `getSeatAvailability` và expiry cleanup; Hoàng dùng response của cả bốn API. QA lập ma trận test theo UC và contract.
3. Chốt migration số mới sau V1 session; schema DB là task riêng của các chủ module. Task này không sửa migration, dependency ứng dụng hoặc security allowlist.
4. Kiểm thử runtime từng UC sau khi hiện thực. Validation OpenAPI/examples chỉ chứng minh tài liệu hợp lệ, không chứng minh endpoint hoặc nghiệp vụ đã chạy.

## Kiểm chứng tài liệu

Đã chạy ngày 12/11/2026:

- `python -m openapi_spec_validator docs/contracts/openapi.yaml` với PYTHONPATH trỏ `tmp/contract-validation`: **OK**, validator **0.9.0**. Công cụ cài riêng trong tmp bị ignore, không thêm dependency ứng dụng hoặc sửa CI.
- Python stdin dùng JSON Schema Draft 2020-12 + FormatChecker + registry resolve refs: **24 lượt response examples** (bảy JSON thành công và các error example được dùng trên từng operation) đều hợp lệ. Kiểm tra chéo giá tổng leg, cutoff 30 phút, ID Trip/Segment, ghế/layout một-to-một và bản đồ hết ghế đều đạt.
- Sáu payload sai bị từ chối đúng: tiền lẻ, FREE có expiry, eligibility false thiếu lý do, UUID sai, timestamp không UTC Z và response lộ field bookingId.
- Review agent chỉ đọc không phát hiện Critical/Important; một Minor về cache headers của response lỗi đã sửa bằng `Cache-Control: no-store` cho toàn bộ responses đã khai báo. Đây không phải approval của thành viên nhóm.

Chưa có validator OpenAPI tự động trong CI. Chưa chạy functional tests cho ba UC vì endpoint chưa được hiện thực. Kết quả schema/example không thay thế test runtime và yêu cầu 15 ca PA4.
