# Team 11 — hướng dẫn cho người và agent

Đọc file này trước khi sửa code. Hướng dẫn công cụ không thay thế contract của repo.

## Đọc theo task
- Luôn đọc `docs/contracts/architecture.md`, `docs/contracts/data-and-api.md` và `CONTRIBUTING.md`.
- Đọc `SETUP_CI.md`, `docs/development.md` và `docs/test/foundation-verification.md` khi sửa build/CI hoặc tiếp nhận bộ khung. Có FE/BE nền tảng tại `src/`, chưa có UC nghiệp vụ; không báo đã hoàn thành PA4 chỉ vì health/build chạy được.
- Tài liệu gốc và bản trích theo trang nằm trong `docs/references.md`. Đọc `pa/PA4/SCOPE.md` khi lập phạm vi PA4; người giao việc đã chốt P04/P05/P07 ngày 12/11/2026, xác nhận cách chấm UC chính với TA vẫn chưa có.
- Đọc UC/BR liên quan và implementation hiện có. Nếu chưa có tài liệu UC trong repo, yêu cầu người giao task cung cấp; không tự đoán nghiệp vụ.
- Chạy `git status` trước khi sửa; giữ nguyên thay đổi của người khác.

## Trước khi code
Nêu UC/task, tiêu chí nghiệm thu, file/module dự kiến sửa, contract bị ảnh hưởng và cách kiểm tra. Task thông thường tiếp tục trong phạm vi đã giao; nếu phát hiện cần đổi contract, báo người phụ trách trước khi sửa các bên liên quan.

## Giới hạn
- Không đổi stack, thêm dependency, đổi cấu hình build/security/CI hoặc refactor ngoài task khi chưa được maintainer thống nhất.
- Không bỏ test, tắt lint/type checking, thêm skip/ignore hoặc dùng `any` để che lỗi. Ngoại lệ phải có lý do cụ thể và review.
- Không commit secret, `.env` thật, dữ liệu khách hàng, output build hoặc log.
- Không tự commit, push, merge, deploy hoặc sửa repository settings nếu task không giao việc đó.
- Không gọi repository/entity nội bộ của module khác; dùng port công bố.
- Không gọi Gemini từ trình duyệt; không tin giá, quyền hạn hay kết quả thanh toán do client gửi.
- Không sửa migration đã được chia sẻ/áp dụng. Thêm migration mới.
- Không tự sửa SAD/UC để hợp thức hóa implementation sai yêu cầu.

## Bàn giao
Liệt kê thay đổi, contract/migration/dependency ảnh hưởng, lệnh thực sự đã chạy và kết quả. Chưa chạy phải ghi chưa chạy; không suy diễn pass. Thành viên gửi PR phải giải thích được request đi qua đâu, dữ liệu thay đổi gì và một nhánh lỗi.

## Kiểm tra
- Source: `src/frontend/`, `src/backend/`; không tạo scripts giả hoặc file giữ chỗ để vượt CI.
- FE: Node theo `.nvmrc`; trong `src/frontend/`: `npm ci`, `npm run lint`, `npm run typecheck`, `npm test`, `npm run build` (PowerShell có thể dùng `npm.cmd`).
- BE: Java 21 + PostgreSQL 16 test riêng; trong `src/backend/`: `./mvnw -B -ntp verify -DfailIfNoTests=true` (Windows: `.\mvnw.cmd -B -ntp verify -DfailIfNoTests=true`). Surefire chạy unit tests, Failsafe chạy `*IT`; không trỏ DB test vào dữ liệu thật.
- Nếu host chưa có JDK: từ gốc repo dùng `docker compose -f compose.test.yaml run --rm backend-test`, sau đó `docker compose -f compose.test.yaml down`. Xem `docs/development.md` cho các chế độ local/dev và giới hạn session/auth hiện tại.
- Thiếu source, wrapper, test, công cụ hoặc môi trường phải báo là chưa kiểm chứng/lỗi đầu vào; không dùng `--if-present`, skip test hoặc điều kiện bỏ job để có CI xanh.
- CI là lớp kiểm tra tối thiểu, không chứng minh tất cả rules đã được tuân thủ.
