# Coach Transportation Management System

Kho mã nguồn và tài liệu dự án nhóm cho học phần Nhập môn Công nghệ Phần mềm.

## Tìm đúng chỗ để làm việc

| Cần làm gì | Xem ở đâu |
|---|---|
| Sửa màn chọn điểm, kết quả, chi tiết hoặc ghế | `src/frontend/src/features/journey/JourneyPages.tsx` |
| Sửa frontend gọi API | `src/frontend/src/shared/api/` |
| Sửa nghiệp vụ backend | `src/backend/src/main/java/vn/edu/team11/coach/` — `trip/` (tìm, chi tiết), `booking/` (ghế), `identity/` (Account, session và security) |
| Viết migration mới | `src/backend/src/main/resources/db/migration/` |
| Xem giao diện FE–BE và điểm ghép Trip–Booking | [OpenAPI](docs/contracts/openapi.yaml) và [TripQueryPort](docs/contracts/trip-query-port.md) |
| Nhận task hoặc giao việc cho agent | [Phân công PA4](pa/PA4/TASKS.md) và [quy tắc cho agent](AGENTS.md) |
| Chạy demo, kiểm thử, xem tài liệu nộp | [Chạy local](docs/development.md), [test plan](docs/test/pa4-test-plan.md), `pa/PA4/` |

[Danh mục tài liệu nguồn](docs/references.md) dẫn tới yêu cầu, SAD và báo cáo đã nộp. Thư mục `pa/PA0/` đến `pa/PA5/` chứa tài liệu theo từng bài tập.

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
- Xem [kết quả CI và bàn giao bộ khung](docs/management/baseline-publication.md). Nhánh `main` được bảo vệ bằng [ruleset Protect main](https://github.com/LienTrungHieu1008/coach-transportation-management-system/rules/27619430).
- Backend đã có bốn GET công khai cho danh mục điểm dừng, tìm chuyến, chi tiết chuyến và trạng thái ghế. Xem [quyết định API](docs/contracts/pa4-api-decisions.md).
- I1 đã có schema Account V2 và tests PostgreSQL. Xem [bàn giao cho Booking](docs/contracts/identity-account.md) và [kết quả kiểm chứng](docs/test/i1-account-verification.md).
