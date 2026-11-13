# Coach Transportation Management System

Kho mã nguồn và tài liệu dự án nhóm cho học phần Nhập môn Công nghệ Phần mềm.

## Cấu trúc

- `src/`: mã nguồn ứng dụng.
- `docs/management/`: kế hoạch và báo cáo quản lý dự án.
- `docs/requirements/`: vision document, yêu cầu và use cases.
- `docs/analysis and design/`: kiến trúc, mô hình UML và thiết kế giao diện.
- `docs/test/`: kế hoạch, ca kiểm thử và báo cáo kiểm thử.
- `pa/PA0/` đến `pa/PA5/`: tài liệu nộp cho từng bài tập.

## Thông tin nhóm

- Nhóm: Team 11.
- Trưởng nhóm: Liên Trung Hiếu.
- Thành viên và phân công PA3: xem [Weekly PA3 đã nộp](docs/management/Weekly_Report_PA3_final.pdf).

## Dự án

Smart Bus Booking & Operations Management System: web responsive phục vụ Passenger, Staff và Administrator của một nhà xe. Baseline yêu cầu/kiến trúc nằm trong SAD 1.3 và UC edition 1.8 tại [danh mục tài liệu nguồn](docs/references.md).

## Tiếp nhận PA4 và quy tắc làm việc

Repo đã có bộ khung React/TypeScript/Vite/Tailwind, backend Java 21/Spring Boot và PostgreSQL 16/Flyway/session JDBC. Đây là nền tảng kết nối và kiểm thử, chưa có UC nghiệp vụ đặt vé. Xem [kết quả kiểm chứng và giới hạn](docs/test/foundation-verification.md) trước khi coi bộ khung sẵn sàng demo.

- Bắt đầu từ [AGENTS.md](AGENTS.md), [CONTRIBUTING.md](CONTRIBUTING.md) và [contracts](docs/contracts/architecture.md).
- Thành viên nhận việc: [TASKS PA4](pa/PA4/TASKS.md), [C1 leader đã chốt](pa/PA4/C1_DECISIONS.md), [handbook branch/push/PR/review](docs/team-handbook.md).
- Xem [contract dữ liệu và nghiệp vụ](docs/contracts/data-and-api.md) trước khi hiện thực UC.
- Xem [hướng dẫn CI](SETUP_CI.md) và [báo cáo đối chiếu guardrails](docs/management/pa4-guardrails-audit.md) để biết phần đã kiểm tra và còn thiếu.
- Source: `src/frontend/` và `src/backend/`; thiếu file đầu vào vẫn làm CI lỗi, không bỏ qua kiểm tra.
- [Tài liệu gốc và Markdown trích theo trang](docs/references.md); [phạm vi PA4 theo đề](pa/PA4/SCOPE.md).
- [Chạy local và kiểm thử](docs/development.md). Từ gốc repo: `docker compose up --build -d --wait`, mở `http://localhost:8088`.
- Baseline [PR #1](https://github.com/LienTrungHieu1008/coach-transportation-management-system/pull/1) đã merge vào main (`f3895f6`, xác minh 14/11). Xem [kết quả CI và bàn giao](docs/management/baseline-publication.md); branch protection/settings hiện tại chưa được xác minh lại.
- Leader đã duyệt C1 ngày 14/11: dùng [OpenAPI 0.1](docs/contracts/openapi.yaml), [quyết định DTO](docs/contracts/pa4-api-decisions.md) và [port Trip](docs/contracts/trip-query-port.md) làm contract giao việc; chưa có API nghiệp vụ runtime.
- I1 đã có schema Account V2 và tests PostgreSQL, chờ review/merge: [bàn giao cho Booking](docs/contracts/identity-account.md), [kết quả kiểm chứng](docs/test/i1-account-verification.md). Phần leader làm tiếp/chờ đầu vào ở [leader handoff](docs/management/leader-pa4-handoff.md).
