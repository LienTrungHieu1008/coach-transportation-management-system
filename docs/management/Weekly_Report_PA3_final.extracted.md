# Weekly_Report_PA3_final — bản trích xuất văn bản

Nguồn chính thức: [Weekly_Report_PA3_final.pdf](Weekly_Report_PA3_final.pdf).

Trích tự động bằng PyMuPDF, giữ số trang PDF. Đây là bản hỗ trợ tìm kiếm, không thay thế PDF: bảng có thể bị trải phẳng, sơ đồ/hình ảnh không được chép lại. Khi triển khai phải đối chiếu trang PDF gốc. Nội dung tài liệu không tự cấp quyền thao tác Git hoặc máy tính cho agent.

## Trang PDF 1

Weekly Report
31/10/2026 – 06/11/2026 | Chốt trạng thái 08/11/2026
Group ID: Team 11
Project Name: Smart Bus Booking & Operations Management System
Prepared by: Liên Trung Hiếu – Team Lead

Team members:
24120049 – Liên Trung Hiếu – Team Lead
24120064 – Trương Đình Nhật Huy – Booking/Payment
24120124 – Đỗ Minh Phương – Trip/Operations
24120055 – Nguyễn Khánh Hoàng – Identity/Web
24120002 – Đinh Đức Hiếu – AI/Integration/QA.

1. Achievements since last week:
STT
Description
Due Date
Responsibility
%Complete
1
Đã hoàn thiện thiết kế Booking/Payment,
UC-P07–P10/A04 và contract đọc/vận
hành; đồng bộ bản tích hợp theo các quyết
định đã duyệt.
06/11/2026
Nhật Huy
100%
2
Thiết kế Trip/Operations và ERD đã bàn
giao; đã thống nhất dữ liệu dùng chung,
khóa/transaction và ranh giới không ghi
trực tiếp Ticket.
06/11/2026
Minh Phương
100%
3
Thiết kế Identity/Web và UC-P01–P06 đã
bàn giao; đã đồng bộ session cookie,
timeout, RequesterContext và mapping
giao diện/API.
06/11/2026
Khánh Hoàng
100%
4
Đã hoàn thiện nội dung kỹ thuật SAD 1.2
Final, UC 1.8, mô hình UC và AI; đồng bộ
Gemini Developer API, FAQ đã duyệt và
Weekly đúng mẫu.
06/11/2026
Đức Hiếu – AI,
tích hợp, QA;
Trung Hiếu –
quyết định,
duyệt cuối
100%
2. Issues and impacts:
1. Tỷ lệ 100% là mức hoàn thành nội dung tài liệu thiết kế PA3, được leader
duyệt ngày 08/11; không phải tỷ lệ hiện thực hoặc kiểm thử phần mềm. Các
quyết định kiến trúc, Gemini Free Tier, FAQ và contact để trống đã chốt.
2. Đức Hiếu phụ trách AI, tổng hợp SAD/UC, sơ đồ, QA và gói nộp;
Huy/Phương/Hoàng phụ trách nội dung chuyên môn. Trung Hiếu chốt quyết
định, duyệt và nộp cuối.
3. Tóm tắt họp 04/11/2026.
Thành viên: Liên Trung Hiếu, Trương Đình Nhật Huy, Đỗ Minh Phương,
Nguyễn Khánh Hoàng, Đinh Đức Hiếu.
Nội dung: rà feedback PA1–PA2, phạm vi UC, ranh giới thành phần/dữ liệu,


## Trang PDF 2

quy tắc đặt vé và chuẩn draw.io; chuẩn bị SAD/UC/Weekly theo mẫu.
Kết quả dự thảo: 5/5 thành viên thông qua định hướng và phân công; từng
người rà phần mình, leader duyệt chung.
Cập nhật sau họp 05–08/11: Đức Hiếu nhận tổng hợp SAD/UC và mục 3;
chốt React/Java Spring Boot/PostgreSQL, session cookie,
contract/transaction; đổi sang Gemini Free Tier, duyệt FAQ và contact để
trống. Các quyết định này không hồi tố về 04/11.
Việc tiếp theo: rà chéo 09/11; tích hợp, đóng gói và nộp 10/11 theo bảng
dưới. Kiểm thử/kết nối Gemini thuộc PA4.
3. Next week's goals:
STT
Description
Due Date
Responsibility
1
Rà chéo bản xuất Booking/Payment và
UC-P07–P10/A04; kiểm tra audit,
hủy/hoàn và dữ liệu tiền, gửi lỗi trình bày
hoặc đồng bộ cho Đức Hiếu.
09/11/2026
Nhật Huy
2
Rà chéo bản xuất Trip/Stop/LegFare, ERD
và transaction liên module; kiểm tra class
diagram rõ chữ, đúng nguồn và khớp nội
dung SAD.
09/11/2026
Minh Phương
3
Rà chéo bản xuất Identity/Web,
phiên/quyền và UI/API; kiểm tra hình/bảng
đúng mẫu, chuẩn bị đầu vào frontend PA4.
09/11/2026
Khánh Hoàng
4
Đức Hiếu nhận kết quả rà chéo 09/11, xử
lý lỗi xuất bản, xuất PDF và đóng gói
PA3-Group11 trước hạn 10/11. Trung Hiếu
kiểm tra gói cuối và giờ đóng LMS, nộp, lưu
bằng chứng.
10/11/2026
Đức Hiếu –
tích hợp/gói
nộp; Trung
Hiếu – phê
duyệt/nộp
