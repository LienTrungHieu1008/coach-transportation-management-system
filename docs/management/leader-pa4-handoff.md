# Phần việc Trung Hiếu — bàn giao 14/11/2026

## C1 và tài liệu giao việc

Leader đã duyệt [C1](../../pa/PA4/C1_DECISIONS.md). [TASKS](../../pa/PA4/TASKS.md) có owner, reviewer, phối hợp và dependency; [handbook](../team-handbook.md) có quy trình nhận việc/Git/PR/review. Chưa ghi thành viên đã nhận việc khi chưa có phản hồi. Việc công bố tài liệu qua nhánh/PR không thay approval implementation hoặc xác nhận TA.

## I1 Account

Phương án: schema `identity_account`, PK `account_id UUID`; email unique không phân biệt casing; role theo SAD; ACTIVE/DISABLED; không default role/account/credential. SQL V2, fixture test riêng, test PostgreSQL và [contract bàn giao](../contracts/identity-account.md). Kết quả xác minh cuối ở [báo cáo I1](../test/i1-account-verification.md); chỉ đánh dấu nghiệm thu sau khi có review của Hoàng và merge.

Huy nhận PK/UUID fixture và constraints từ contract, Phương lấy V2 trên main trước khi merge/apply V3. Không copy migration của nhánh chưa merge rồi báo là prerequisite đã có trên main.

## C2: đã chuẩn bị, chờ endpoint thật

Không mở allowlist cho endpoint chưa được triển khai. Không thêm controller/advice rỗng để báo đã tích hợp. Mỗi khi API sẵn sàng, chủ API gửi leader:

```text
Task / PR / commit:
Method + path cần public:
Query/DTO đã khớp OpenAPI:
Business errors module đã xử lý:
Phần security/error chung cần ghép:
Request mẫu, fixture và expected chính/lỗi:
Tests đã chạy và điểm đang vướng:
```

Leader cùng owner thống nhất PR chứa thay đổi chung; chỉ permit GET chính xác theo contract, không permit cả `/api/**`. Review JSON error/status theo OpenAPI nhưng không tự đổi body 401/403 nền tảng ngoài task. Chạy verify với DB test; kiểm tra GET public không bị yêu cầu login, writes/private routes vẫn bị bảo vệ. Sau đó ghép FE từng màn, kiểm tra Segment giữ nguyên và dữ liệu lỗi không hiển thị thành công. Đức Hiếu review tích hợp; owner sửa nghiệp vụ module mình.

**Chưa hoàn thành C2:** chưa có P2/P3/P4/H2 và FE nghiệp vụ để kiểm chứng flow. I1 pass không chứng minh các route đó hoạt động.

## C3: đầu vào cần nhận, không tự tạo kết quả

- [ ] Từ TA/Moodle: mẫu test plan/weekly và feedback PA3; xác nhận P04/P05/P07 đáp ứng cách hiểu UC chính.
- [ ] Từ mỗi thành viên: task/PR/commit, việc thực tế từ 07/11, ca test/bằng chứng và phần SAD thuộc module.
- [ ] Từ Đức Hiếu: test report ít nhất 15 ca thực thi, bug/retest và bản SAD PA4 tổng hợp.
- [ ] Từ Hoàng: prototype home + ba màn, mô tả mục đích/thông tin/cách dùng và flow nối API thật.
- [ ] Diễn tập trên build/DB/dữ liệu được ghi rõ; kiểm tra gói PA4-Group11 và giờ đóng LMS; leader duyệt/nộp/lưu bằng chứng.

Nội dung để leader hỏi TA (bản nháp, chưa gửi):

> Nhóm 11 dự kiến PA4 demo luồng P04 tìm chuyến → P05 xem chi tiết → P07 xem ghế theo Segment, với DB thật và tối thiểu 5 test cases cho mỗi UC. Nhờ thầy/cô xác nhận luồng này đáp ứng mục “ít nhất một UC chính” hay cần bao gồm tạo Booking. Nhóm cũng cần mẫu test plan/weekly PA4 và feedback SAD PA3 để cập nhật đúng yêu cầu.

Chưa có report nghiệp vụ thì không ghi test pass, không dựng weekly hoặc SAD hoàn thành bằng công việc dự kiến. Agent không tự nhắn TA/thành viên, nộp LMS hoặc merge thay leader.
