# F1 — sửa tests và ngày demo, 17/11/2026

Phạm vi: bản sửa trên `feat/f1-prototype`, dựa trên PR #7 / `3419e54`. Chỉ sửa prototype và tests frontend; không đổi API contract, dependency, backend, migration hoặc CI. Kết quả dưới đây là kiểm tra local; kiểm tra GitHub CI theo đúng commit bản sửa trên PR #7, không dùng kết quả của commit cũ.

## Nguyên nhân và cách sửa

App đã thay màn foundation health bằng flow prototype nhưng bộ test vẫn tìm loading/ready/retry của màn cũ. Thay test App bằng tương tác thật qua P04/P05/P07; giữ kiểm tra client health còn tồn tại trong `shared/api/health.test.ts` (UP, network failure, HTTP error và payload lỗi). Không phục hồi màn health chỉ để thỏa kỳ vọng cũ, không skip/tắt kiểm tra.

Snapshot mẫu có ngày 20/11/2026 nhưng form so với ngày máy, khiến sau ngày này không thể đi qua luồng mẫu. F1 nay dùng `demoDetails.serverTime` làm clock mô phỏng và hiện nhãn ngày mô phỏng ngay trên form. Vẫn chặn ngày trước ngày mô phỏng. Không thay UUID, thời điểm/giá trong snapshots hoặc giả là dữ liệu live. Khi nối API thật, F2–F4 phải dùng dữ liệu/thời gian có thẩm quyền từ server thay clock prototype.

## Kiểm chứng thực tế

- Trước khi sửa production: test đặt ngày máy 21/11/2026 bắt được form từ chối ngày mẫu; lần chạy RED sau khi sửa selector có 6 failed / 8 passed. Một lần chạy trước đó bị selector trùng tên nút điều hướng và nút form, đã sửa selector theo phạm vi form.
- Sau sửa: Node 24.19.0; `npm.cmd ci` đạt (226 packages, audit không có vulnerability tại thời điểm chạy).
- `npm.cmd run lint`: đạt, không warning.
- `npm.cmd run typecheck`: đạt. Lần đầu phát hiện option `exact` không hợp lệ với query theo role trong test; đã bỏ và chạy lại đủ checks.
- `npm.cmd test`: 2 files / **15 tests pass**, không skip. 8 tests App: ngày máy sau ngày mẫu, flow đi/quay lại và trạng thái ghế, Stop trùng, ngày quá khứ mô phỏng, kết quả rỗng, eligibility, lỗi tìm/retry, lỗi availability không giả FREE. 7 tests health client.
- `npm.cmd run build`: đạt, Vite build thành công.

Ổ C hết dung lượng trong phiên kiểm tra trước; lần này dùng TEMP/TMP/npm cache trên ổ D cho các lệnh npm. Không thay cấu hình repo để né lỗi môi trường.

## Chưa kiểm chứng

Chưa chạy lại backend local (không đổi BE), chưa kiểm tra trực quan bằng trình duyệt desktop/mobile. GitHub CI được theo dõi riêng trên PR #7. Các tests dùng DOM/jsdom và dữ liệu mô phỏng, không thay 15 functional cases thực thi với API/DB thật của PA4. Hoàng/thành viên khác cần review trước merge.
