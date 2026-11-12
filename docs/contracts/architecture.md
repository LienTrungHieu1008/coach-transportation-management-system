# Contract kiến trúc — Team 11

Nguồn: SAD v1.3 ngày 09/11/2026 và UC edition 1.8 được lưu qua `docs/references.md`; bộ guardrails/handoff là bối cảnh ban đầu. Đã đối chiếu các phần nền tảng liên quan bộ khung, chưa kiểm toán mọi sơ đồ/UC. Phân biệt baseline thiết kế với các khả năng đã hiện thực dưới đây.

## Baseline đã chốt trong SAD
- FE: React, TypeScript, Vite, Tailwind CSS.
- BE: Java 21, Spring Boot, Maven; modular monolith, một DB PostgreSQL.
- Persistence: Spring Data JPA kết hợp SQL, Flyway; xác thực bằng Spring Security và Spring Session JDBC.
- Giao tiếp FE/BE: JSON qua HTTPS, cùng origin qua reverse proxy trong môi trường demo; Vite proxy có thể dùng khi phát triển.
- Module backend: Identity & Access; Trip & Operations; Booking & Payment; AI Assistance.
- Docker Compose cho môi trường demo. PA4 thanh toán mock có nhãn MÔ PHỎNG; PA5 dự kiến VNPay Sandbox.
- AI chỉ đọc, gọi Gemini từ backend qua gateway. Chưa dùng Redis, message broker hay microservices.

## Bố cục và bộ khung hiện tại
Repo có `src/frontend/` (React/TypeScript/Vite/Tailwind), `src/backend/` (Java 21/Spring Boot 3.5.16), `docs/`, `pa/PA0/` đến `pa/PA5/`, `.github/`. Các `.gitkeep` ban đầu được giữ lại.

Node 24.19.0 theo `.nvmrc`; Maven Wrapper 3.3.4 tải Maven 3.10.0. PostgreSQL major 16 thống nhất giữa `compose.yaml`, `compose.test.yaml` và CI. Các lựa chọn version ngoài Java là quyết định hiện thực của bộ khung, không phải tất cả đã được SAD chỉ định.

Base package `vn.edu.team11.coach`. Dành tên module `identity`, `trip`, `booking`, `ai`; mới có hạ tầng security/CSRF thuộc `identity`. Không tạo entity/service/port nghiệp vụ rỗng để giả có đủ module. Theo SAD, Trip & Operations phục vụ truy vấn công khai P04/P05; ownership/quyền được hiện thực cùng UC tương ứng.

Compose local dùng Nginx phục vụ FE và proxy `/api/` sang backend, chỉ công bố web trên loopback 8088. Vite dev proxy tới backend 8080; `compose.dev.yaml` mở port loopback riêng cho phát triển. Đây là HTTP local; chưa có deployment HTTPS.

Flyway V1 tạo bảng Spring Session JDBC từ schema PostgreSQL upstream; session mặc định 30 phút, HttpOnly/SameSite=Lax. Chưa có account/login hoặc logic timeout theo role Staff/Admin 15 phút. Health/CSRF là API kỹ thuật; chưa có UC nghiệp vụ, Gemini hoặc payment gateway. Xem `docs/development.md` và báo cáo kiểm chứng cho kết quả đã chạy.

## Quy tắc module khi hiện thực

Trong backend, mỗi module có `api` (HTTP controller/DTO), `application` (điều phối use-case), `domain` (nghiệp vụ), `infrastructure` (JPA, SQL, provider), `contract` (port/DTO công bố cho module khác).

- Domain không phụ thuộc controller, Spring Data repository hoặc external provider.
- Controller kiểm tra cấu trúc input và gọi application service; nghiệp vụ và transaction nằm phía backend.
- Module khác chỉ import `contract`; không import entity/repository nội bộ hoặc truy vấn trực tiếp bảng của module khác.
- Port có đầu vào/đầu ra rõ, lỗi nghiệp vụ và yêu cầu transaction. Không lộ JPA entity qua HTTP hoặc port.
- `shared` chỉ chứa kiểu/kỹ thuật thật sự dùng chung; không gom nghiệp vụ vào một service chung.
- Một database không đồng nghĩa module nào cũng được ghi mọi bảng.
- Giao dịch liên module chỉ được coi là nguyên tử khi dùng cùng DataSource, transaction manager và REQUIRED theo SAD. Không gọi provider khi đang giữ khóa.

Frontend chia theo feature; API client, session/CSRF và ánh xạ lỗi dùng chung. Không lặp fetch/axios và xử lý xác thực ở từng màn hình; không thêm state library/UI library thứ hai khi chưa thống nhất.

## Thực thi tự động và review
CI kiểm tra file đầu vào rồi chạy FE lint/typecheck/Vitest/build và BE Maven verify với PostgreSQL. Failsafe được gắn integration-test/verify; test nền tảng chưa thay thế test nghiệp vụ PA4. Khi hiện thực các module, bổ sung ArchUnit kiểm tra module/layer, lint hạn chế import ở FE và test nghiệp vụ; chưa có các kiểm tra đó thì review bằng tay, không ghi là đã tự động chặn. Workflow đã chạy thành công trên GitHub cho baseline `efa1d0f`, xem `docs/management/baseline-publication.md`; required gate trong branch protection chưa được cấu hình.
