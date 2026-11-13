# Kiểm chứng I1 Account — 14/11/2026

Phạm vi: [I1 plan](../superpowers/plans/2026-11-14-i1-account-schema.md), [contract Account](../contracts/identity-account.md). V2 thêm prerequisite dữ liệu theo C1; V1 giữ nguyên, không API login/register, không đổi dependency/security/CI.

## RED trước V2

Đã viết `AccountSchemaIT` và fixture trước migration. Chạy từ gốc repo:

```powershell
docker compose -f compose.test.yaml run --rm backend-test
```

Lệnh chạy wrapper `clean verify -DfailIfNoTests=true` trên Java 21/PostgreSQL 16 test riêng, DB tmpfs không port công khai. Exit 1 đúng RED: 35 invocations AccountSchemaIT thất bại do chưa có bảng/migration V2; 5 unit và 3 FoundationIT vẫn pass, 0 skipped. Tests biên dịch được; không phải lỗi compile/tooling. XML/log RED giữ trong `tmp/i1-execution/` bị gitignore.

## GREEN trên DB mới

Lượt đầu sau V2 đạt 34/35 Account tests; test FK dùng bảng TEMP tham chiếu bảng thường nên PostgreSQL từ chối. Đã sửa riêng test probe thành bảng tạo trong transaction và rollback, không nới FK hoặc sửa DDL để né lỗi. Sau đó chạy `docker compose -f compose.test.yaml down` để bỏ DB tmpfs cũ, rồi chạy lại toàn bộ lệnh verify ở trên.

Lượt DB mới **exit 0, BUILD SUCCESS**: **35 AccountSchemaIT + 3 FoundationIT + 5 SecurityConfigurationTests = 43 tests**, failures/errors/skipped đều 0; XML Surefire/Failsafe xác nhận. Java 21/PostgreSQL 16.15 thật, wrapper clean verify với failIfNoTests=true. Không skip test hoặc H2. Lệnh `docker compose -f compose.test.yaml down` sau cùng exit 0, dọn services/network test, không xóa cache Maven hoặc volume dev.

Đã kiểm chứng: DB mới V1→V2; V1 có session và attributes vẫn được giữ sau upgrade; chạy lại migrate không apply lặp; PK UUID, null/blank, email trùng khác casing, role/status; FK thật nhận Account tồn tại và từ chối Account không tồn tại; fixture DISABLED tách migration và rollback. Không có Account seed trong migration/runtime.

Log còn cảnh báo Mockito dynamic agent và metadata repository như baseline; không tắt warning để báo xanh. FE source không đổi nên chưa chạy lại FE checks trên host cho riêng I1; CI sẽ kiểm tra cả hai phía khi PR được mở.

## Review / remote

Review agent độc lập phát hiện một Important: test lịch sử V1→V2 lại migrate tới latest nên V3/V4 tương lai có thể làm test thất bại. Đã thêm regression tạo migration mới hơn trong thư mục test tạm (không đặt vào classpath ứng dụng), quan sát RED `expected 1 but was 2`, rồi giới hạn riêng historical upgrade bằng target V2. Test app khởi động trên DB mới vẫn chạy latest của ứng dụng, không pin app/CI về V2. Chạy lại toàn bộ clean verify trên DB tmpfs mới **43/43 pass,0 skipped**, XML xác nhận. Migration V2 không thay đổi sau khi thêm.

Một Minor được ghi nhận: CHECK btrim hiện kiểm tra dấu cách thông thường, chưa kiểm tra toàn bộ whitespace/email/password policy; normalization đầy đủ được để lại P01/P02, giới hạn ghi trong contract Account. Không thay đổi V2 để che giới hạn đó. Review agent không thay review/approval của Hoàng.

Báo cáo này ghi kết quả local và review source. Khi xem PR, kiểm tra CI của đúng commit mới nhất; không suy ra từ baseline hoặc run local. Chưa merge và chưa có approval thành viên. Phạm vi không thay dependency/security/CI hoặc triển khai endpoint UC nghiệp vụ.

## Ý nghĩa và giới hạn

- Chứng minh DDL/constraints/fixture/FK và upgrade session khi tests đạt; không chứng minh P01/P02, ownership hoặc UC-P04/P05/P07.
- Fixture DISABLED chứa sentinel không phải credential dùng được. DB chỉ kiểm tra password_hash nonblank, không xác minh thuật toán hay chính sách mật khẩu tương lai.
- Email CI-unique, hai status và tên bảng/PK là quyết định hiện thực I1 ghi rõ cho reviewer; không coi C1 là approval DDL của Hoàng.
- C2 chờ endpoint; C3 chờ TA/Moodle/feedback và report nghiệp vụ. Checklist/bản hỏi TA chuẩn bị ở [leader handoff](../management/leader-pa4-handoff.md), chưa gửi ra ngoài.
- Chưa thay migration đã chia sẻ, không seed account ở startup/production và không chạm dữ liệu khách hàng.
