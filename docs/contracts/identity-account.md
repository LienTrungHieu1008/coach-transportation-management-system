# I1 — Account prerequisite cho Booking

Ngày 14/11/2026. Phạm vi đã giao tại [C1](../../pa/PA4/C1_DECISIONS.md), task I1 trong [TASKS](../../pa/PA4/TASKS.md). Nguồn yêu cầu: SAD 1.3 trang 17 (Account/Role), UC 1.8 P01/P02 (trường bắt buộc, email duy nhất, active/disabled). Chi tiết DDL dưới đây là lựa chọn hiện thực của I1, cần Hoàng review; không tự sửa SAD để coi các chi tiết này đã được PDF quy định.

## Contract dữ liệu bàn giao

Identity sở hữu bảng **`identity_account`**, PK **`account_id UUID`**. Booking V4 tham chiếu `passenger_account_id UUID REFERENCES identity_account(account_id)`; ownership cardinality theo SAD. Kiểu FK và tên cột Booking cụ thể thuộc PR V4, không được bỏ FK hoặc dựng một bảng Account khác để né dependency.

| Cột Account | Kiểu/ràng buộc | Ghi chú |
|---|---|---|
| account_id | UUID, PK, NOT NULL | Không có default; caller/provision cung cấp UUID. |
| email | TEXT, NOT NULL, nonblank, không space đầu/cuối | Unique index `uq_identity_account_email_ci` trên `lower(email)`; khác hoa/thường vẫn trùng. DB giữ cách viết đã nhận, không tự lowercase mọi email. |
| password_hash | TEXT, NOT NULL, nonblank | DB không xác minh thuật toán/hash hợp lệ và không thay CredentialPolicy. Không chứa mật khẩu thật trong migration/fixture. |
| full_name | TEXT, NOT NULL, nonblank | Không tự đặt giới hạn chiều dài ngoài yêu cầu hiện có. |
| phone | TEXT, NOT NULL, nonblank | Không unique; không tự áp đặt regex/đầu số điện thoại mới. |
| role | TEXT, NOT NULL, CHECK | PASSENGER / STAFF / ADMINISTRATOR; enum logic bằng CHECK, không cần native PostgreSQL enum/extension. |
| status | TEXT, NOT NULL, CHECK | ACTIVE / DISABLED; biểu diễn tối thiểu active/inactive của UC. Không dùng LOCKED thay cho throttling tạm thời. |

Không tự thêm default role/status hoặc tạo account mặc định. Case-insensitive uniqueness và hai status là quyết định hiện thực để reviewer biết, không phải claim đã có đăng ký/đăng nhập. Normalization/validation email đầy đủ, chính sách mật khẩu và chuyển trạng thái account thuộc task P01/P02 tương lai; không coi CHECK nonblank là đã kiểm tra toàn bộ input nghiệp vụ.

Giới hạn DDL hiện tại: `btrim` mặc định kiểm tra rỗng/chỉ dấu cách thông thường và space quanh email; không loại toàn bộ tab/newline/Unicode whitespace hoặc xác minh định dạng email/password hash. Đây là giới hạn đã được review ghi nhận, phần normalization/validation P01 chưa triển khai; không được dùng I1 làm bằng chứng input account đã được kiểm tra đầy đủ. Không âm thầm sửa V2 sau khi chia sẻ/áp dụng; nếu bổ sung DB constraint phải cấp migration mới theo quy trình.

## Migration và dữ liệu mẫu

- V1 session giữ nguyên. V2 chỉ tạo bảng/index/constraints, không seed account, không đổi session hoặc security allowlist. V3 Trip và V4 Booking merge/apply sau V2 theo thứ tự đã chốt.
- Fixture tại `src/backend/src/test/resources/fixtures/identity_accounts.sql` không phải migration hoặc production seed. Nó tạo hai Passenger DISABLED với sentinel `!DISABLED_DEMO_NO_CREDENTIAL`, không phải mật khẩu/hash đăng nhập dùng được. Không bật account hoặc đưa sentinel này vào CredentialPolicy như một hash thật.
- Account One: `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1`, email `pa4.passenger.one@example.invalid`.
- Account Two: `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2`, email `pa4.passenger.two@example.invalid`.
- Huy dùng các UUID trên cho fixture FK Booking, không tạo credentials hoặc Account riêng. Account DISABLED vẫn có thể là chủ dữ liệu lịch sử/demo; việc công khai availability không kiểm tra quyền đăng nhập của chủ Booking. Không được suy ra họ có thể tạo Booking mới hoặc đăng nhập.
- Fixture được nạp vào transaction test bằng `@Sql` và rollback sau test. Nếu nạp thủ công cho demo, phải chọn DB local/test được chỉ định đã có V2, nạp trước fixture Booking; không trỏ vào dữ liệu thật. Dùng DB sạch hoặc xác định rõ fixture đã nạp; file cố ý báo trùng ID/email thay vì im lặng ghi đè.

## Tests và phần chưa được làm

`AccountSchemaIT` kiểm tra migration DB mới, upgrade V1 có session/attributes sẵn, chạy lại Flyway không mất dữ liệu, null/blank, PK/email unique, role/status, fixture và FK PostgreSQL thực. Bảng FK probe được tạo trong transaction test rồi rollback (PostgreSQL không cho bảng TEMP tham chiếu bảng thường); không phải implementation Booking hoặc bảng được tạo bởi migration.

Chưa có Account HTTP API, JPA entity/repository không dùng, login/register, timeout theo role hoặc ownership service. Tests I1 không thay test P01/P02 hoặc 15 functional cases PA4. Kết quả lệnh thực tế xem [báo cáo I1](../test/i1-account-verification.md) khi bàn giao.
