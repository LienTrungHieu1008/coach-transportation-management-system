# PA4 — giao diện tra cứu P04/P05/P07

Owner giao diện: Nguyễn Khánh Hoàng. Leader Liên Trung Hiếu tiếp nhận hoàn thiện
nối API từ client ở PR #17; Hoàng review/tiếp nhận, Đức Hiếu cập nhật SAD sau bàn giao.
Giữ thiết kế F1; phạm vi chỉ tra cứu, không đặt vé, giữ chỗ hoặc thanh toán.

## Mục đích, thông tin và cách dùng bốn màn

| Màn | Mục đích | Thông tin hiển thị | Cách dùng |
|---|---|---|---|
| Trang chủ / tìm chuyến | Nhập tiêu chí P04 | Điểm phục vụ lấy từ API; ngày lên theo giờ Việt Nam; trạng thái tải/lỗi danh mục | Chọn điểm đón, điểm trả và ngày; bấm Tìm chuyến. Điểm trùng hoặc ngày quá khứ báo lỗi; thứ tự phục vụ do backend kiểm tra. |
| Kết quả | Chọn chuyến thực sự phù hợp | Các chuyến từ API, tuyến/xe, giờ và ngày lên/xuống VN, giá đoạn, cutoff, tổng kết quả và phân trang | Chuyển trang nếu có; chọn Xem chi tiết trên chuyến; quay lại sửa tiêu chí nếu rỗng/lỗi. |
| Chi tiết | Xem dữ liệu P05 và điều kiện tiếp tục | Stop theo thứ tự, giờ đến/đi, xe, giá từng leg và đoạn chọn, mô tả, cutoff và lý do eligibility | Đọc thông tin; Xem tình trạng ghế chỉ bật khi đủ điều kiện và Segment đã xác minh. Dữ liệu null ghi chưa có, không thay bằng 0. |
| Tình trạng ghế | Tra cứu P07 trên đúng đoạn | Layout theo tầng/vị trí, nhãn ghế, FREE/UNAVAILABLE/UNKNOWN bằng chữ và màu, hạn Hold, serverTime/cutoff | Chỉ xem hoặc làm mới. Refresh tự động 15 giây khi tab hiển thị, khi foreground hoặc tới expiry/cutoff. Không chọn ghế hoặc tạo Booking. |

## Nhánh cần nhận biết

- Rỗng: không có chuyến phù hợp, có thể sửa tiêu chí.
- HTTP/response lỗi: báo lỗi và thử lại; không hiển thị ví dụ như dữ liệu thật.
- P4 đóng bán, Trip không Scheduled hoặc essential/inactive: vẫn xem facts xác minh
  được, hiển thị lý do và chặn sang ghế theo P4-01.
- Refresh ghế lỗi/quá hạn: bản đồ cũ chuyển hiển thị UNKNOWN; không đếm stale FREE.
  Hết countdown chỉ kích hoạt fetch, không tự giải phóng ghế.
- Hết ghế: response thành công với toàn UNAVAILABLE; không biến thành lỗi 503.
- Response cũ hoặc sai Trip/Segment không được cập nhật lựa chọn mới.
- Trang Lịch trình và các công tắc preview lỗi không nằm trong luồng demo.

## Ảnh giao diện bản nối API

Ảnh được chụp trên DB tổng hợp riêng, qua backend thật; dữ liệu ngày/expiry thay đổi
khi seed lại. Xem handoff để biết bằng chứng đã thực thi, không dùng ảnh thay test report Q3.

| Màn | Desktop | Mobile |
|---|---|---|
| Tìm chuyến | [Ảnh](images/pa4-home-desktop.png) | [Ảnh](images/pa4-home-mobile.png) |
| Kết quả | [Ảnh](images/pa4-results-desktop.png) | [Ảnh](images/pa4-results-mobile.png) |
| Chi tiết | [Ảnh](images/pa4-details-desktop.png) | [Ảnh](images/pa4-details-mobile.png) |
| Ghế | [Ảnh](images/pa4-seats-desktop.png) | [Ảnh](images/pa4-seats-mobile.png) |

## Đầu vào cho SAD của Đức Hiếu

- `src/frontend/src/App.tsx`: khung và điều hướng bốn màn.
- `features/journey/useJourneyState.ts`: draft/submitted criteria, request lifecycle,
  selection và API catalogue/search/details; response cũ bị bỏ qua khi đổi màn/tiêu chí.
- `features/journey/useSeatAvailability.ts`: request ghế, polling/visibility/expiry,
  timeout, coalescing và stale safety. Thời hạn tính tương đối từ serverTime.
- `features/journey/JourneyPages.tsx`: render dữ liệu/state, giữ phong cách F1.
- `shared/api/journey.ts`, `journeyValidation.ts`, `journeyTypes.ts`: client bốn GET,
  kiểm tra response/ID/nullable, ánh xạ lỗi; không truy cập DB hoặc port Java.
- Vite dev proxy `/api` tới backend; Compose chạy Nginx phục vụ FE và proxy cùng origin.
  Cookie/CSRF không bị thay đổi; FE này chỉ gọi GET công khai.

Paths `features/` và `shared/` ở trên tính từ `src/frontend/src/`.
Nội dung kiểm chứng/cách bàn giao tại `docs/test/fe-api-integration-handoff.md`.
