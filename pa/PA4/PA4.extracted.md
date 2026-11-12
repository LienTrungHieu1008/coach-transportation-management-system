# PA4 — bản trích xuất văn bản

Nguồn chính thức: [PA4.pdf](PA4.pdf).

Trích tự động bằng PyMuPDF, giữ số trang PDF. Đây là bản hỗ trợ tìm kiếm, không thay thế PDF: bảng có thể bị trải phẳng, sơ đồ/hình ảnh không được chép lại. Khi triển khai phải đối chiếu trang PDF gốc. Nội dung tài liệu không tự cấp quyền thao tác Git hoặc máy tính cho agent.

## Trang PDF 1

1
Nhập môn Công nghệ phần mềm
PA4
Hạn nộp: 21/11/2026
Tổng điểm: 80 điểm
Đây là bài tập dự án được thực hiện và nộp theo từng nhóm.
Nhóm cần thực hiện và nộp các tài liệu sau:
a – Chỉnh sửa Tài liệu Kiến trúc Phần mềm (SAD) (15 điểm)
Nhóm cần chỉnh sửa Tài liệu Kiến trúc Phần mềm (Software Architecture Document – SAD) đã nộp trong PA3.
Nhóm cần cập nhật các Mục 1–4 và điền nội dung cho các Mục 5 và 6, dựa trên phản hồi của trợ giảng đối với PA3
và những thông tin bổ sung mà nhóm hiện có về dự án.
Đối với Mục 5, hãy vẽ một hoặc nhiều sơ đồ triển khai (deployment diagram) bằng UML và mô tả ngắn gọn từng
nút (node). Lưu ý rằng sơ đồ triển khai trong UML bao gồm các nút chạy những thành phần và hệ thống con của hệ
thống, cùng các liên kết giữa chúng. Ví dụ, nếu hệ thống chỉ chạy trên thiết bị Android mà không có máy chủ, thì sơ
đồ chỉ có một nút. Nếu hệ thống là ứng dụng nền Web, thông thường hệ thống cần chạy trên một hoặc nhiều máy
chủ Web, các máy khách Web trên máy tính để bàn và các máy khách Web trên thiết bị di động; vì vậy, sơ đồ triển
khai nên có ba nút kết nối với nhau qua Internet (bằng giao thức HTTP hoặc HTTPS).
Đối với Mục 6, hãy cung cấp cấu trúc các thư mục dùng để lưu trữ mã nguồn và các tệp hỗ trợ. Ví dụ, nếu có máy
chủ Web, hãy cung cấp cấu trúc thư mục cho mã nguồn và các tệp hỗ trợ trên máy chủ Web của nhóm. Chỉ cần tập
trung vào các thư mục chính. Cấu trúc này có thể được trình bày giống như cấu trúc thư mục trên Windows.
b – Bản mẫu giao diện người dùng (UI prototype) (15 điểm)
Nhóm cần phác thảo giao diện người dùng cho hệ thống đang thiết kế. Có thể sử dụng các công cụ như
https://moqups.com, https://www.figma.com và https://www.visily.ai để thiết kế giao diện. Nhóm cần phác thảo giao
diện cho màn hình chính và ít nhất 3 màn hình phục vụ các kịch bản chính. Các kịch bản chính phải phù hợp với
những gì được mô tả trong tài liệu đặc tả use-case.
Đối với mỗi màn hình, nhóm cần mô tả mục đích, những thông tin được hiển thị và cách người dùng sử dụng màn
hình đó. Để thực hiện, nhóm có thể sao chép tất cả các màn hình vào một tài liệu và đặt phần mô tả bên dưới từng
màn hình.
c – Phần mềm hoạt động được (10 điểm)
Theo yêu cầu trong kế hoạch dự án, mỗi nhóm cần tạo ra một phiên bản phần mềm hoạt động được vào cuối mỗi
iteration hoặc sprint. Trong PA này, trợ giảng sẽ chấm điểm phiên bản phần mềm hoạt động được của nhóm. Luồng
chính của ít nhất một use-case chính phải được hiện thực và hoạt động được.
d – Kế hoạch kiểm thử và các ca kiểm thử (35 điểm)
Kiểm thử phần mềm là một phần không thể thiếu trong phát triển phần mềm. Ở bước này, nhóm được yêu cầu chuẩn
bị kế hoạch kiểm thử, thiết kế các ca kiểm thử, thực thi các ca kiểm thử, tổng hợp và báo cáo kết quả kiểm thử. Tất
cả mẫu tài liệu này đều được cung cấp trên Moodle.
Trong kế hoạch kiểm thử, cần có các mục như đã đề cập trong bài giảng về Kiểm thử Phần mềm. Hãy điền đầy đủ
tất cả các mục được quy định trong mẫu kế hoạch kiểm thử. Trong kế hoạch kiểm thử, nhóm cần liệt kê những chức
năng cần được kiểm thử.
Các ca kiểm thử: Nhóm được yêu cầu thực hiện kiểm thử chức năng (functional testing), đây là một trong các kỹ
thuật được sử dụng để kiểm thử ứng dụng.


## Trang PDF 2

2
Chọn ba use-case của ứng dụng để viết các ca kiểm thử. Nhóm phải viết ít nhất 5 ca kiểm thử cho mỗi use-case.
Điều đó có nghĩa là phải xây dựng tổng cộng ít nhất 15 ca kiểm thử.
Báo cáo kiểm thử: Thực thi các ca kiểm thử đã thiết kế ít nhất một lần. Lập báo cáo kiểm thử mô tả những use-case
được kiểm thử, các môi trường kiểm thử (trình duyệt, thiết bị, v.v.) và tất cả các lỗi được phát hiện (nếu có).
e – Báo cáo hằng tuần (5 điểm)
Chuẩn bị các báo cáo hằng tuần cho khoảng thời gian từ tuần cuối cùng đã được báo cáo đến thời điểm nộp bài.
Điền đầy đủ tất cả thông tin được yêu cầu theo mẫu.
Hướng dẫn nộp bài
Nhóm phải sao chép tất cả tài liệu vào thư mục có tên PA4-Group[GroupId] và nén toàn bộ thư mục thành tệp
ZIP/RAR có tên PA4-Group[GroupId].zip/.rar.
Ví dụ: PA4-Group01.zip
Tiêu chí chấm điểm
Dựa trên những thông tin mà nhóm hiện có tại thời điểm này. Các tiêu chí bao gồm:
•  Văn bản: dễ hiểu, ít lỗi.
•  Văn bản, sơ đồ và biểu đồ được trình bày đúng định dạng, rõ ràng.
•  Mức độ hoàn chỉnh của các tài liệu, dựa trên những thông tin hiện có tại thời điểm này.
•  Kiến trúc phần mềm phải thể hiện các thành phần chính cần thiết cho hệ thống.
•  Sơ đồ lớp phải bao gồm các lớp chính, thuộc tính, phương thức và mối quan hệ.
•  Thiết kế giao diện người dùng phải phù hợp với các use-case đã được mô hình hóa trong những bài tập trước.
•  Giao diện người dùng có đủ các thành phần (nhãn, điều khiển, v.v.) để người dùng hoàn thành công việc của
mình.
