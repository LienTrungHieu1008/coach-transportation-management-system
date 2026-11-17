# F1 — kiểm chứng thiết kế mới, 17/11/2026

Theo hướng navy–cam được người giao việc chọn, dùng UI UX Pro Max cho hierarchy, responsive, typography và contrast. Chỉ sửa `src/frontend/src/index.css` và `src/frontend/src/features/journey/journey.css`; giữ nguyên JSX/handlers/state/mock data, DTO/API contract và dependency. Không coi thay giao diện là hoàn thành F2–F4 nối BE.

## Thay đổi nhìn thấy

- Header rõ trạng thái điều hướng, hero navy với minh họa xe tiết chế và form trắng nổi bật.
- Typography hệ thống hỗ trợ tiếng Việt, thông tin phụ từ 12px, control cao tối thiểu 44px; nhãn input và focus không bị mất khi đổi breakpoint.
- Thẻ kết quả nhấn giờ/giá, timeline chi tiết và cột tóm tắt thống nhất; sơ đồ ghế giữ ID/tọa độ/trạng thái cũ, chỉ xem.
- Giữ nhãn mô phỏng, ngày snapshot, các preview states, trang Schedule, dark mode và reduced-motion. Form xếp dọc trên điện thoại, cột tóm tắt xuống hàng theo DOM.

## Đã chạy

- Baseline trước thay CSS: 2 files, 15 tests pass.
- Sau sửa: `npm.cmd ci`, `npm.cmd run lint`, `npm.cmd run typecheck`, `npm.cmd test` (15/15), `npm.cmd run build`; không thêm skip/ignore. Lần cài lại đầu tiên bị EPERM do Vite giữ native module trên Windows; đã dừng đúng server do agent khởi chạy rồi chạy lại.
- `git diff --check` kiểm tra whitespace; UTF-8/fences của tài liệu được kiểm tra riêng.
- Chrome headless qua Python Playwright đã cài sẵn, không thêm dependency vào repo: đi Home → Results → Details → Seats → Schedule ở 1440×1000, 1024×900, 768×1024, 375×812. **20 tổ hợp màn/viewport không tràn ngang**, mọi button hiển thị cao ít nhất 44px.
- Chụp và xem ảnh render đại diện desktop/tablet/mobile; kiểm tra dark Home/Seats/error ở 375px. Lỗi availability không giữ lại ghế FREE. Chạy context reduced-motion và đo focus outline 3px solid trên select.
- Đo các cặp màu chính: trắng trên CTA cam 5.18:1; text phụ light trên trắng 6.33:1; text chính trên nền light 13.14:1; text phụ dark trên surface 8.56:1. Đây không phải kiểm toán toàn bộ WCAG.

Ảnh và kết quả kiểm tra tạm nằm trong `tmp/f1-visual/` (ignored); script kiểm tra tự động là công cụ QA tạm, không thay tests/CI chính thức. CUA không khởi tạo được do ổ C đầy, nên dùng Chrome headless với TEMP/cache trên ổ D. Không xóa dữ liệu người dùng hoặc thay config CI để xử lý môi trường.

## Giới hạn

Chưa kiểm tra thiết bị thật, Safari/Firefox, screen reader hoặc mọi tổ hợp zoom/landscape. Không chạy lại backend local vì không đổi BE; không kiểm thử API/DB thật trong task thiết kế. Người dùng đã xem và đồng ý bản thiết kế, yêu cầu công bố lên PR ngày 17/11/2026; GitHub CI cần kiểm tra theo đúng commit bản thiết kế trên PR #7. Thành viên review trước merge.
