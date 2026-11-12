# PA4: yêu cầu và phạm vi đã chốt

Nguồn: [đề PA4 gốc](PA4.pdf), trang 1–2. Hạn nộp **21/11/2026**, tổng **80 điểm**. Tài liệu dưới đây giúp lập kế hoạch, không thay thế đề hoặc phản hồi của trợ giảng.

## Phần bắt buộc theo đề

| Hạng mục | Điểm | Yêu cầu bàn giao |
|---|---:|---|
| SAD | 15 | Sửa mục 1–4 theo feedback PA3/thông tin mới; điền mục 5 bằng deployment diagram UML + mô tả node; mục 6 là cấu trúc thư mục source/tệp hỗ trợ chính. |
| UI prototype | 15 | Màn hình chính **và ít nhất 3 màn hình** cho kịch bản chính, khớp UC. Mỗi màn có mục đích, thông tin hiển thị và cách sử dụng. |
| Phần mềm chạy được | 10 | Luồng chính của **ít nhất 1 UC chính** thực sự hoạt động. Bộ khung/health endpoint chưa đáp ứng mục này. |
| Kiểm thử | 35 | Test plan điền đủ mẫu Moodle; chọn **3 UC**, ít nhất **5 ca/UC = 15 ca**; thực thi ít nhất một lần và báo cáo UC, môi trường, lỗi/kết quả thật. |
| Weekly | 5 | Nối tiếp tuần cuối đã báo cáo đến lúc nộp, đúng mẫu. |

Đề không yêu cầu hiện thực toàn bộ 20 UC ở PA4. Tuy nhiên phần test yêu cầu thực thi test của 3 UC, nên chỉ hiện thực một UC rồi đánh dấu test các UC còn lại pass sẽ không đáp ứng. Chọn 3 UC vừa sức chạy được là phương án lập kế hoạch; nếu nhóm muốn phạm vi khác cần hỏi trợ giảng, chưa có xác nhận ngoại lệ.

Đóng gói tất cả sản phẩm vào thư mục `PA4-Group11`, nén `PA4-Group11.zip` hoặc `.rar`. File tài liệu tham khảo trong repo chưa phải gói nộp PA4 hoàn chỉnh.

## Phạm vi đã chốt ngày 12/11/2026

Người giao việc đã xác nhận chọn ba UC dưới đây trong phiên ngày 12/11/2026. Triển khai một luồng công khai liên tục, chưa phụ thuộc đăng nhập/thanh toán/AI:

1. **UC-P04 Search Trips**: tìm theo Stop lên/xuống và ngày lên; dữ liệu Trip thực trong PostgreSQL, cutoff tại Stop đầu tuyến, ngày theo `Asia/Ho_Chi_Minh`, fare từ các leg.
2. **UC-P05 View Trip Details**: xem thông tin chuyến/đoạn đã chọn, các Stop và giá thực theo đặc tả.
3. **UC-P07 View Seat Availability**: xem trạng thái ghế thực trên Segment; thao tác này chỉ đọc, không tạo Booking/hold hoặc bảo đảm ghế cho bước đặt sau.

Đối chiếu [UC edition 1.8](../../docs/requirements/Team%2011%20-%20rup_ucspec-v2_final.pdf), đặc biệt P04 trang 10–12, P05 trang 12–13 và P07 trang 15–16 trước khi hiện thực. Quyết định này xác nhận phạm vi làm việc, không phải bằng chứng TA đã duyệt cách chấm “UC chính”. Leader xác nhận điểm này với TA; nếu bắt buộc luồng đặt vé, cần đánh giá lại phụ thuộc P01/P02/P08/P09.

Phân công và mốc bàn giao đề xuất: [kế hoạch nhóm](WORK_PLAN.md). Tên người phụ trách dựa trên vai trò PA3; phân công chưa được từng thành viên xác nhận.

Prototype tương ứng có thể gồm Trang chủ, Kết quả tìm chuyến, Chi tiết chuyến và Sơ đồ ghế. Cần thực hiện đủ flow/nhánh test theo UC đã chọn; không dùng màn hình mẫu hoặc dữ liệu hardcode để báo là phần mềm hoàn chỉnh.

## Những gì bộ khung hiện tại cần bàn giao

- Frontend React/TypeScript/Vite/Tailwind với lint/typecheck/test/build thật.
- Backend Java 21/Spring Boot/Maven, PostgreSQL 16, migration Flyway cho session JDBC; health và cơ chế lấy CSRF token cho SPA.
- Docker Compose cùng origin qua reverse proxy, kiểm tra nền tảng HTTP/security/DB.
- Chưa có UC-P04/P05/P07, schema nghiệp vụ, authentication theo P01/P02, payment mock hoặc gateway Gemini. Không tính test nền tảng thành 15 functional test cases PA4.

## Checklist còn phải hoàn thiện

- [x] Người giao việc chốt UC-P04/P05/P07 ngày 12/11/2026.
- [ ] Chốt tiêu chí demo với nhóm và xác nhận cách hiểu UC chính với TA.
- [ ] Nhận feedback PA3 và mẫu test/weekly Moodle; chưa có các đầu vào này trong phiên.
- [ ] Cập nhật SAD 1–6 bằng revision PA4 riêng; không ghi đè PDF PA3 đã nộp.
- [ ] Hoàn thiện ít nhất 4 màn prototype, kèm mô tả theo đề.
- [ ] Hiện thực và demo UC đã chốt với DB thật, dữ liệu thử có thể tái tạo.
- [ ] Test plan + tối thiểu 15 ca/3 UC, thực thi, ghi bug và kết quả thật.
- [ ] Weekly tiếp theo bắt đầu sau kỳ **31/10–06/11** của bản PA3; ghi đúng công việc thực tế từ 07/11, phân biệt cập nhật chốt 08/11 với kỳ báo cáo. Không suy diễn Weekly PA3 đã báo cả tuần 07–13/11.
- [ ] Rà gói `PA4-Group11` trước hạn 21/11/2026; chưa tự nộp LMS.
