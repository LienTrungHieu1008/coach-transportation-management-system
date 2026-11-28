# PA5 - Quyết định contract đã duyệt

Liên Trung Hiếu đã duyệt P08 hai bước, bốn mẫu xe A01 và năm điểm ghép còn lại dưới đây. Phạm vi PA5 là đủ 20 UC mục tiêu, gồm P04/P05/P07 đã hiện thực, và C01 nằm trong S03. Mốc và người phụ trách ở [bảng phân công Jira](JIRA_HANDOFF.md).

## 1. Identity và session (P01–P03)

- Đăng ký công khai chỉ tạo Passenger. Nhận họ tên, email, số điện thoại, mật khẩu và xác nhận mật khẩu. Thành công thì chuyển sang đăng nhập. Tài khoản Staff/Admin được cấp riêng.
- `POST /api/v1/auth/login` nhận `email`, `password`, tạo session cookie và trả `accountId`, `fullName`, `role`. Role dùng đúng tên `PASSENGER`, `STAFF`, `ADMINISTRATOR`.
- `GET /api/v1/auth/session` trả thông tin phiên để UI khôi phục khi tải lại; phiên thiếu hoặc hết hạn trả `401`. `POST /api/v1/auth/logout` hủy phiên, trả `204`. Áp dụng hạn phiên không hoạt động 30 phút cho Passenger, 15 phút cho Staff/Admin và các quy tắc đăng nhập sai/tài khoản bị vô hiệu của P02.
- Tái dùng `GET /api/session/csrf` và header `X-CSRF-TOKEN` cho thao tác ghi. UI lấy lại token sau khi đăng nhập hoặc đăng xuất thành công. Phân biệt `401` hết/thiếu phiên với `403` sai quyền hoặc CSRF. Bốn GET của P04/P05/P07 vẫn công khai.

## 2. Xem lại và xác nhận giữ ghế (P08)

| Bước | Request | Kết quả |
|---|---|---|
| `POST /api/v1/bookings/preview` | `tripId`, `fromStopId`, `toStopId`, `seats` gồm 1–6 mục `{physicalSeatId, travellerName}` | Backend kiểm tra lựa chọn, trả giờ lên/xuống, nhãn ghế, thông tin liên hệ từ tài khoản, giá mỗi ghế và tổng VND. Bước xem lại không ghi Booking hay giữ ghế. |
| `POST /api/v1/bookings` | Cùng thông tin lựa chọn và `expectedTotalAmountVnd` vừa hiển thị | Backend tính lại giá và xác minh ghế trong giao dịch tạo Booking. Thành công trả `201` với `bookingId`, `bookingReference`, `status: Pending`, `totalAmountVnd`, `serverTime`, `expiresAt`. |

- Cả hai bước yêu cầu session Passenger. Backend lấy chủ Booking và liên hệ từ tài khoản đang đăng nhập; tự xác minh chặng, giá VND, ghế thuộc xe của chuyến, cutoff tại điểm đầu tuyến và phân bổ hiện hành. ID ghế phải khác nhau. Kết quả P07 là trạng thái để hiển thị; P08 luôn kiểm tra lại khi ghi.
- Backend dùng `expectedTotalAmountVnd` để kiểm tra người dùng đã thấy đúng giá. Nếu giá đổi, trả `409 PRICE_CHANGED` và UI cho xem lại. Nếu một ghế xung đột, trả `409 SEAT_CONFLICT`, UI tải lại sơ đồ để chọn lại. Booking và toàn bộ hold được ghi nguyên tử. Hạn giữ 10 phút bắt đầu khi tạo thành công; tải lại và thanh toán lại không kéo dài hạn.
- Phương và Huy chốt bổ sung giờ lên/xuống và giá đoạn vào contract Trip dùng cho P08. `TripSeatFacts` hiện đã có chặng, cutoff và layout, chưa có giờ lên/xuống hoặc giá. Huy gọi port công bố của Trip; Hoàng dùng response P08 để nối UI.

## 3. Danh mục mẫu xe (A01)

Quyết định PA5 mở rộng danh mục hai mẫu trong BR-A01-01 thành bốn mẫu định sẵn. Giữ mô hình Admin chọn mẫu có sẵn, số ghế do backend xác định từ số vị trí ghế của mẫu.

| Loại xe | Sức chứa của mẫu |
|---|---:|
| Ghế ngồi | 29 |
| Ghế ngồi | 45 |
| Limousine | 16 |
| Giường nằm | 34 |

- Phương bàn giao mã mẫu và danh sách ghế theo mẫu. Mỗi xe có sơ đồ với `seatLayoutId` và `physicalSeatId` của chính xe đó. UI dựng ghế từ dữ liệu trả về và Booking xác minh ghế thuộc xe được gán cho Trip. Sức chứa xe tách biệt với giới hạn 1–6 ghế mỗi Booking.
- Giữ BR-A01-02 đến BR-A01-04. Sau khi xe đã được gán cho bất kỳ Trip nào, cấm thay đổi cấu trúc/sức chứa và giữ nguyên ID ghế. Vô hiệu hóa xe ngăn gán chuyến mới, bảo toàn chuyến và vé hiện có. Chỉ xóa xe chưa từng được Trip tham chiếu.
- Kiểm tra số ghế của cả bốn mẫu. Chạy P07/P08 với ít nhất hai mẫu có sức chứa khác nhau, gồm ca từ chối gửi ID ghế của xe khác. Bộ demo dùng địa danh/tên bến thật và lịch, xe, giá tự sinh, có thể nạp lại từ DB mới. Giữ fixture A/B/C cho hồi quy.

## 4. Thanh toán thử và phát vé (P09)

- VNPay Sandbox do backend tích hợp và xác minh kết quả. Redirect trình duyệt chỉ đưa người dùng về UI xem trạng thái. Mỗi Booking tối đa ba lần thử, mỗi lúc tối đa một lần `Pending`; kết quả chưa xác định vẫn phải được xử lý theo P09 trước khi cho thử lại.
- Chỉ lần thanh toán hợp lệ đầu tiên, trong lúc Booking còn `Pending` và hold/phân bổ còn đủ điều kiện, mới xác nhận Booking và phát đúng một vé cho mỗi ghế. Thông báo lặp không phát thêm vé. Tách trạng thái thanh toán, Booking, vé và đối soát để UI có thể hiển thị đúng.
- Khoản trả thành công nhưng đến muộn, đến sau khi hủy hoặc là khoản dư phải đưa vào đối soát theo P09 và ghi nhận toàn bộ khoản cần hoàn. Không khôi phục phân bổ đã mất hay phát vé cho khoản này.

## 5. Vé, tra cứu và check-in (P06/S02/S03/C01)

- P06 chỉ cho Passenger đọc Booking/vé của chính mình. S02 cho Staff tra theo mã Booking, mã vé hoặc số liên hệ theo quyền.
- S03 dùng mã vé, Trip được chọn và thông tin điểm lên đã xác minh. C01 kiểm tra quyền, vé hợp lệ, Booking đã xác nhận, đúng chuyến và điều kiện lên xe. Chạy lại C01 ngay trước khi ghi check-in để xử lý yêu cầu đồng thời và ngăn check-in lặp.
- Huy cung cấp dữ liệu/vé fixture để Đức Hiếu phát triển song song, sau đó bàn giao mã Booking/vé được P09 phát thật **trước 12:00 ngày 05/12/2026**, giờ Việt Nam, để nghiệm thu S02/S03.

## 6. Hủy Booking, hủy chuyến và xem tổng quan (P10/S04/A04)

- P10 hủy cả Booking. Booking `Pending` được phép hủy trước xác nhận; chưa trả tiền thì không cần hoàn. Với `Confirmed`, phải còn ít nhất 12 giờ trước giờ lên tại Stop đã chọn và chưa có vé check-in. Tính hoàn 90% một lần trên tổng giá vé đã trả, làm tròn half-up đến VND.
- S04 hủy chuyến `Scheduled` phải ghi lý do và người thực hiện. Hủy các Booking `Pending`/`Confirmed` bị ảnh hưởng, giải phóng ghế và vô hiệu vé, kể cả vé đã check-in. Với Booking đã thanh toán, ghi yêu cầu hoàn 100%, có đối soát khoản hoàn đã ghi để không hoàn trùng hoặc vượt khoản đã trả.
- Hoàn tiền thực hiện ngoài ứng dụng theo UC. A04 chỉ đọc dữ liệu và trạng thái Booking/vé/Payment/Refund/đối soát. Khoản thanh toán đến sau khi hủy đi theo P09.

## 7. AI và dữ liệu được phép đọc (P11/P12)

- P11 lấy chuyến và giá từ dữ liệu thật trong hệ thống. Kết quả AI chỉ giúp diễn giải tiêu chí tìm kiếm; backend xác minh tiêu chí và truy vấn Trip. P12 trả lời FAQ công khai; thông tin Booking cá nhân yêu cầu đăng nhập và chỉ đọc dữ liệu do backend xác minh thuộc người dùng.
- Gọi Gemini qua backend, không gửi thông tin định danh. Không tạo Booking, hold, thanh toán hoặc cam kết ghế qua AI. Giữ giới hạn dùng chung theo đặc tả và hiển thị phương án tiếp tục khi Gemini hoặc dữ liệu nguồn lỗi.

## 8. Chi tiết các owner cần bàn giao theo PA5-COORD

Các quyết định trên đã được duyệt. Các owner ghi chi tiết kỹ thuật theo bảng trong mốc **29/11**, ưu tiên **P09 → S03**. Mỗi API/port ghi quyền, request/response hoặc DTO mẫu, lỗi, side effect và yêu cầu giao dịch/xử lý lặp khi áp dụng.

| Điểm ghép | Owner hoàn thiện | Cần ghi tiếp |
|---|---|---|
| P01–P03 | Trung Hiếu, Hoàng | Route đăng ký, JSON và lỗi đăng ký/đăng nhập/CSRF, thông tin phiên. |
| P07 → P08 | Huy, Phương, Hoàng | JSON đầy đủ của hai bước, lỗi theo từng nhánh, DTO Trip cho giờ và giá, hành vi khi gửi lại xác nhận sau lỗi mạng. |
| A01 → P07/P08 | Phương, Hoàng, Huy | Mã mẫu, sơ đồ từng mẫu, JSON cho Admin, fixture và ca ghế thuộc xe khác. |
| P09 → P06/S02/S03 | Huy, Đức Hiếu, Hoàng, Phương | Dữ liệu kết quả thanh toán, mã vé/Booking, các trạng thái, DTO đọc nội bộ và lỗi check-in; vé fixture và vé phát thật để nghiệm thu. |
| P10/S04/A04 | Trung Hiếu, Huy, Phương, Đức Hiếu | Trách nhiệm của từng module, giao dịch khi hủy, dữ liệu Refund/đối soát cho A04 và ca xử lý đồng thời. |
| P11/P12 | Hoàng, Phương, Huy, Trung Hiếu | FAQ được duyệt, danh sách trường được đọc, dữ liệu bị loại trước khi gửi Gemini và lỗi/fallback UI. |

Nguồn trong repo gồm [UC Spec 1.8](../../docs/requirements/Team%2011%20-%20rup_ucspec-v2_final.pdf), [contract dữ liệu/API](../../docs/contracts/data-and-api.md), [OpenAPI PA4](../../docs/contracts/openapi.yaml) và [TripQueryPort](../../docs/contracts/trip-query-port.md).
