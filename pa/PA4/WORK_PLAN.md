# Phân công PA4 — cập nhật 14/11/2026

Phạm vi UC-P04/P05/P07 đã được người giao việc chốt tại [SCOPE](SCOPE.md). Leader đã duyệt [C1](C1_DECISIONS.md) ngày 14/11 và giao cập nhật phân công; vai trò kế thừa [Weekly PA3](../../docs/management/Weekly_Report_PA3_final.pdf). Chưa xác nhận lịch rảnh/cam kết từng thành viên. Hạn nộp 21/11/2026; các mốc là mục tiêu nội bộ, không phải công việc đã hoàn thành.

Đã chia thành task giao trực tiếp tại [TASKS.md](TASKS.md), cập nhật 14/11/2026: mỗi task có người làm, phụ thuộc, đầu ra và reviewer, gồm I1 Account V2 mới được duyệt. Dùng TASKS cho mốc hiện tại và [handbook](../../docs/team-handbook.md) cho quy trình; bảng bên dưới giữ vai trò tổng thể.

TASKS có mục Phối hợp làm việc (owner file, bàn giao, dependency) và checklist riêng cho Trung Hiếu. I1 Account được ưu tiên; C2 security/error chung ghép tăng dần theo từng endpoint sẵn sàng, không chờ đến ngày demo. Review và QA không chuyển trách nhiệm sửa bug khỏi tác giả module.

## Chủ trì và đầu ra

| Thành viên | Phần chủ trì | Đầu ra nghiệm thu | Người review |
|---|---|---|---|
| Liên Trung Hiếu | Account prerequisite, điều phối, review và demo | I1 Account V2 (không login); security/error mapping chung khi tích hợp; xác nhận UC chính với TA; nhận feedback/mẫu Moodle; phân bổ migration; weekly từ 07/11; duyệt gói nộp/demo | Hoàng review I1; Đức Hiếu review tích hợp |
| Đỗ Minh Phương | Backend Trip: P04 + P05 | Schema/migration Trip/Stop/LegFare theo SAD; API tìm chuyến và chi tiết; dữ liệu demo tái tạo; unit/integration tests cho ngày lên, thứ tự Stop, cutoff, giá và nhánh lỗi; cập nhật thiết kế phần Trip | Nhật Huy; Trung Hiếu duyệt contract |
| Trương Đình Nhật Huy | Backend Booking: P07 | API đọc trạng thái ghế theo Segment; xử lý giao nhau/trạng thái/hold hết hạn đúng UC; fixture test có trạng thái ghế khác nhau; unit/integration tests; công bố port đọc cho bên cần, không truy cập repository Trip | Minh Phương; Trung Hiếu duyệt contract |
| Nguyễn Khánh Hoàng | Frontend và prototype xuyên suốt ba UC | Home + kết quả tìm chuyến + chi tiết + sơ đồ ghế; nối API thật; trạng thái loading/rỗng/lỗi và điều hướng giữ Segment đã chọn; mô tả mục đích/thông tin/cách dùng mỗi màn; FE tests | Đức Hiếu, các chủ UC đối chiếu dữ liệu |
| Đinh Đức Hiếu | QA độc lập, tích hợp, SAD/gói nộp | Tổng hợp test plan theo mẫu Moodle; ít nhất 15 ca, mỗi UC ít nhất 5; thực thi trên build tích hợp, ghi lỗi và retest; SAD revision PA4 mục 1–6, deployment/folder structure từ implementation; kiểm tra gói nộp | Hoàng review UI/test; chủ module review SAD; Trung Hiếu duyệt cuối |

Mỗi chủ UC cung cấp nhánh chính/nhánh lỗi và ca kiểm thử cho phần mình. Đức Hiếu tổ chức thực thi và rà chéo; không giao toàn bộ việc nghĩ test, viết tài liệu và sửa lỗi cho QA. Mỗi người cập nhật SAD/weekly phần mình, người tổng hợp chỉ hợp nhất và kiểm tra nhất quán. Chưa giao hiện thực AI, đăng nhập hoặc thanh toán trong phạm vi này.

## Thứ tự và mốc đề xuất

| Mốc | Việc phải bàn giao |
|---|---|
| 14/11 | Leader duyệt C1 kỹ thuật, phát task/handbook; Trung Hiếu bắt đầu I1, Phương/Huy chuẩn bị schema/logic/tests, Hoàng prototype, Đức Hiếu test plan. Thành viên xác nhận nhận việc và lịch thực tế. |
| 15–16/11 | Ưu tiên V2→V3→V4, dữ liệu/port để ghép; backend + tests, bốn màn theo contract, QA plan/cases/SAD nháp. Mock FE chỉ hỗ trợ phát triển, phải ghi nhãn và thay bằng API thật trước nghiệm thu. |
| 17/11 | Tích hợp xuyên suốt trên Compose: tìm → chọn chuyến → xem chi tiết → xem ghế đúng Segment. Kiểm tra có dữ liệu, không có kết quả, input sai, lỗi tải và các nhánh UC bắt buộc. |
| 18–19/11 | Thực thi ít nhất 15 ca trên build tích hợp; ghi môi trường/phiên bản/dữ liệu/kết quả thật; sửa bug và retest. Hoàn thiện SAD, prototype và weekly. |
| 20/11 | Review chéo; chạy lại kiểm tra bắt buộc; diễn tập demo; rà và đóng gói `PA4-Group11`. |
| 21/11 | Leader kiểm tra giờ đóng LMS, nộp và lưu bằng chứng; việc nộp do người được giao thực hiện. |

## Điểm ghép bắt buộc

- P04 trả định danh chuyến và đoạn lên/xuống đủ cho P05/P07 theo contract đã duyệt; FE không tự tính giá hoặc suy diễn ghế trống.
- Phương sở hữu dữ liệu Trip; Huy sở hữu logic phân bổ ghế. Liên module dùng port công bố, không gọi entity/repository của nhau. Chốt ownership seat layout và truy vấn Segment từ SAD trước khi code.
- P07 chỉ đọc, không tạo Booking/hold; xem ghế trống không bảo đảm giữ được ghế. Chưa có UC ghi: tạo fixture test/demo rõ ràng, không giả là đã thực hiện đặt/thanh toán.
- Migration nghiệp vụ thêm sau V1 session hiện có; không sửa V1 đã áp dụng. Dữ liệu demo/test tách dữ liệu thật, có cách khởi tạo lại.
- Mỗi task/PR ghi UC, file/module, contract/migration ảnh hưởng, tiêu chí nghiệm thu và lệnh kiểm tra. Tác giả sửa lỗi phần mình; reviewer là người khác.
- Baseline PR #1 đã merge main (`f3895f6`, fetch xác nhận 14/11); xem [bàn giao](../../docs/management/baseline-publication.md). Contract C1 đã được leader duyệt; thành viên lấy main mới nhất và nhận bản tài liệu C1/TASKS/handbook được chia sẻ trước khi code. Chưa ghi approval cá nhân khi chưa có phản hồi.

## Hoàn thành một task khi nào?

Code/artefact đúng UC, có hướng dẫn tái hiện, nhánh lỗi và kiểm tra thực sự đã chạy; một thành viên khác review. Backend verify với PostgreSQL test riêng; frontend lint/typecheck/test/build theo [AGENTS](../../AGENTS.md). Functional cases ghi kết quả thật, không dùng kết quả health hoặc unit tests thay cho việc thực thi 15 ca PA4. Chỉ đánh dấu hoàn thành sau khi đầu ra được kiểm tra, không dựa vào số file hoặc phần trăm tự khai.
