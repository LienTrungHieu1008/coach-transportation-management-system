# Quy trình đóng góp

1. Một issue/task nêu UC, phạm vi và tiêu chí nghiệm thu. Chia nhỏ theo hành vi có thể kiểm tra, tránh PR gom nhiều việc.
2. Tạo branch `feat/<task>-<mo-ta>`, `fix/<task>-<mo-ta>` hoặc `chore/<mo-ta>` từ main cập nhật.
3. Đọc AGENTS và contract. Thành viên chịu trách nhiệm code do agent tạo; agent không là người review độc lập.
4. Chạy kiểm tra liên quan; thêm test khi thay đổi nghiệp vụ, phân quyền, giao dịch hoặc sửa lỗi. Không viết test chỉ để gọi lại implementation.
5. Mở PR theo mẫu; cung cấp dữ liệu demo/steps tái hiện và bằng chứng thực tế. Không gửi secret trong log/ảnh.
6. Một người khác review; Hiếu quản lý merge theo phân công. PR do Hiếu viết vẫn cần người khác review.
7. Sửa xong và đủ required checks/review mới merge; ưu tiên squash. Không push trực tiếp main.

Mỗi PR nghiệp vụ: tác giả giải thích được điểm vào API/UI, service xử lý, dữ liệu thay đổi, transaction và ít nhất một nhánh lỗi. Reviewer kiểm tra khớp UC và tác động liên module; CI xanh không thay thế review.

Thay đổi contract, dependency, migration, security hoặc CI phải được đánh dấu trong PR và review kỹ phần ảnh hưởng. Không tự bỏ bước kiểm tra để kịp deadline.
