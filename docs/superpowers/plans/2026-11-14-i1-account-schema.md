# I1 Account Schema Implementation Plan

**Goal:** Bàn giao prerequisite Account thật cho Booking: Flyway V2, test PostgreSQL, fixture không có login và contract PK/constraints. C1 đã được leader duyệt; người dùng giao hoàn thành phần khả thi của mình ngày 14/11.

**Architecture:** Identity sở hữu `identity_account`; Booking V4 tham chiếu `account_id` bằng FK. I1 dùng SQL/JDBC integration tests; chưa cần controller/JPA entity/service không dùng. Không đổi V1, source module khác, stack/dependency, security hoặc CI.

**Spec:** `pa/PA4/C1_DECISIONS.md`, `pa/PA4/TASKS.md` I1; SAD 1.3 trang 17 Account/Role; UC 1.8 P01/P02 về trường bắt buộc/email duy nhất/active và disabled. Chi tiết DDL do I1 cụ thể hóa, review bởi Hoàng.

## Decisions

- Bảng `identity_account`, PK UUID `account_id`. Email text trimmed/nonblank, unique index trên lower(email), không thêm citext extension. Full name/phone/password hash nonblank NOT NULL; role PASSENGER/STAFF/ADMINISTRATOR. Không đặt giới hạn độ dài/regex contact hoặc thuật toán mật khẩu chưa được spec chốt vào DDL.
- Status `ACTIVE/DISABLED` là lựa chọn hiện thực tối thiểu cho active/inactive của UC, cần được reviewer biết. Không thêm trạng thái locked thay cơ chế login throttling tạm thời.
- Không tự sinh ID/role/status mặc định; caller/provision tương lai phải khai báo. Fixture tổng hợp DISABLED với sentinel không phải password/hash đăng nhập dùng được; không account mặc định trong migration/runtime.
- C2 chỉ chuẩn bị bàn giao: chưa có endpoint nghiệp vụ để mở public hoặc map lỗi. C3 chưa có report/feedback/mẫu Moodle để hoàn tất; không tự tạo kết quả.

## Tasks and verification

- [x] Viết `src/backend/src/test/java/vn/edu/team11/coach/identity/infrastructure/AccountSchemaIT.java` và fixture `src/backend/src/test/resources/fixtures/identity_accounts.sql` trước V2.
- [x] Chạy backend verify với Compose test riêng; xác nhận RED do thiếu bảng/migration, không phải lỗi compile/tooling.
- [x] Thêm `src/backend/src/main/resources/db/migration/V2__identity_account.sql`; chạy full verify GREEN, không skip. Test DB mới và upgrade V1 có session sẵn, chạy lại migration không mất dữ liệu; null/blank/role/status/unique/FK; fixture riêng. 43 tests,0 fail/error/skipped.
- [x] Tài liệu `docs/contracts/identity-account.md` cho Huy/Phương; báo cáo kiểm chứng và trạng thái TASKS/C1 đúng phần đã làm/chưa nghiệm thu. Chưa thực hiện code task thành viên khác.
- [x] Review độc lập theo requesting-code-review; Important về historical migration target được kiểm chứng RED→GREEN với future migration test, final full suite43/43 pass. Minor normalization đầy đủ để lại P01/P02 và ghi giới hạn trong contract. Chuẩn bị commit/PR I1 theo task, không merge hoặc tự ghi approval thành viên.

## Review focus

Email trùng khác casing; migration không nạp credential; FK Account không bị bỏ để giả demo; session V1 không đổi/mất khi upgrade; tests thật trên PostgreSQL 16 và không tạo login/role trust từ client. Những giới hạn chưa thực hiện (normalization policy P01 đầy đủ, login, role timeout) phải ghi rõ, không tuyên bố I1 đáp ứng P01/P02.
