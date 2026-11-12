# Contract dữ liệu, API và nghiệp vụ

Nguồn: bộ guardrails/handoff và tài liệu gốc trong `docs/references.md`. Đã đọc các phần nền tảng liên quan bộ khung, chưa kiểm toán toàn bộ UC/BR. Repo có health/CSRF và test nền tảng; chưa có API nghiệp vụ hoặc test chứng minh các quy tắc Booking/Payment dưới đây.

## Baseline từ SAD/UC
- ID nghiệp vụ UUID; khóa JSON camelCase.
- Thời điểm lưu UTC; hiển thị/lọc theo Asia/Ho_Chi_Minh. Ngày tìm chuyến theo Stop lên.
- Tiền VND là số nguyên; không dùng float để tính tiền. Phí/giá cuối cùng do backend quyết định.
- Cookie session HttpOnly, Secure khi HTTPS, SameSite=Lax; CSRF cho request thay đổi dữ liệu. Không tự đổi sang JWT/localStorage.
- Session Passenger hết hạn sau 30 phút không hoạt động; Staff/Admin 15 phút.
- Kiểm tra role và ownership ở backend cho từng thao tác; không tin accountId/role client tự khai.
- Booking 1 Trip, 1 Segment, 1–6 ghế khác nhau; mỗi ghế có tên hành khách.
- Hold 10 phút bắt đầu khi tạo thành công; reload/retry không kéo dài. Không có hai phân bổ hiệu lực trùng ghế trên Segment giao nhau.
- Tạo Booking và holds nguyên tử. Không tạo phân bổ thành công một phần.
- Từ chối đặt mới trong 30 phút trước giờ khởi hành tại Stop đầu tuyến; không dùng giờ lên ở Stop đã chọn để thay cutoff này. Đối chiếu UC gốc cho quy ước tại đúng mốc thời gian.
- Payment tối đa 3 attempts, tối đa 1 Pending; thông báo lặp không phát hành vé lặp. PA4 mock phải ghi rõ mô phỏng, không nhận tiền thật.
- Redirect từ client không là bằng chứng thanh toán; kết quả thanh toán phải được backend xác minh qua gateway theo chế độ mock/sandbox tương ứng.
- Hủy cả Booking; Confirmed yêu cầu còn ít nhất 12 giờ trước giờ lên tại Stop đã chọn và chưa check-in. Hoàn 90% tổng tiền, half-up một lần; xử lý hoàn tiền nằm ngoài app theo UC.
- Hủy chuyến bởi nhà xe là UC riêng, hoàn 100%; không áp dụng quy tắc hủy Booking 90% cho trường hợp này.
- Đối chiếu UC gốc cho mọi cutoff/trạng thái/nhánh lỗi; danh sách này không thay thế đặc tả.

## API mới: thống nhất trước khi hai phía code
Đặt đặc tả API nghiệp vụ tại `docs/contracts/openapi.yaml` trước khi hai phía triển khai. Người giao việc đã chốt UC-P04/P05/P07 ngày 12/11/2026, xem `pa/PA4/SCOPE.md`. Hiện chỉ có `GET /api/health` và `GET /api/session/csrf` phục vụ bộ khung (xem `docs/development.md`), chưa có endpoint/schema nghiệp vụ; không dùng file API giả làm baseline.

Mỗi endpoint phải có: UC/BR nguồn, method/path, role/ownership, request và validation, response ví dụ, status/lỗi, side effect, tính lặp an toàn/concurrency khi cần. Quy định rõ missing/null, enum, paging, thời gian và tiền.

Đề xuất JSON lỗi chung, cần chốt trước endpoint đầu tiên:
`{"code":"SEAT_UNAVAILABLE","message":"Ghế vừa được giữ bởi người khác","fieldErrors":[],"traceId":"..."}`
Message phục vụ hiển thị; FE xử lý theo code/status. Không trả stack trace, SQL hoặc secret.

Status đề xuất: 400 input sai; 401 chưa đăng nhập; 403 thiếu quyền; 404 không tồn tại/không được tiết lộ; 409 xung đột trạng thái/ghế; 500 lỗi nội bộ. Chốt nhất quán trong OpenAPI.

## Thay đổi contract
PR ghi rõ contract cũ/mới, bên bị ảnh hưởng, migration và cập nhật FE/BE/test tương ứng. Maintainer và người phụ trách phần phụ thuộc review trước merge. Không tự đổi tên trường, enum hoặc port trong PR chức năng mà không báo.
