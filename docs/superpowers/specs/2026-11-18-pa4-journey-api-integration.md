# FE PA4 — nối API P04/P05/P07, bản thiết kế để leader review

Trạng thái: Trung Hiếu duyệt kế hoạch và giao triển khai ngày 18/11/2026.
Kết quả thực thi tại `docs/test/fe-api-integration-handoff.md`; chưa tự merge.

## Bối cảnh và người nhận

- Main kiểm tra ngày 18/11: `5ce55ee`, đã có bốn GET public sau #16.
- #17, head `d556cd2`, chỉ thêm client và bản PROGRESS cũ ở gốc repo;
  page/state vẫn dùng demoData. CI fail TS1294 ở constructor JourneyApiError.
- Leader tiếp nhận hoàn thiện FE; Hoàng review, đọc lại code và tiếp nhận;
  sau đó chuyển cập nhật FE cho Đức Hiếu sửa SAD. Đây là luồng dự kiến, chưa ghi xong.
- Leader xác nhận các owner đã gửi nội dung SAD/weekly. Không giao gửi lại;
  nội dung FE chỉ gửi bổ sung sau khi implementation thay đổi.

## Cách làm đề xuất

Ưu tiên nhánh `feat/pa4-journey-api-integration` từ main mới nhất, worktree riêng.
Tiếp nhận riêng client `journey.ts` từ #17, ghi nguồn đóng góp Hoàng trong handoff/PR;
không lấy PROGRESS cũ ở gốc. Giữ #17 để tra cứu, không tự đóng hoặc sửa nhánh Hoàng.
Phương án khác là sửa trực tiếp #17; không chọn mặc định vì leader nhận triển khai
và cần tách phần bàn giao/review. Chưa commit/push/tạo PR khi chỉ lập kế hoạch.

## Phạm vi và tiêu chí

1. Giữ giao diện F1, responsive/dark/focus; chỉ chỉnh thành phần cần để dùng dữ liệu thật.
   Bốn màn: Home/search → Results → Details → Seats; ẩn Lịch trình theo quyết định leader.
2. Client gọi bốn GET qua `/api/v1`; same-origin, no-store, AbortSignal.
   Không đổi API/backend/port, migration, dependency, tsconfig, security hoặc CI.
3. Danh mục Stop lấy server; ngày mặc định theo hôm nay tại Asia/Ho_Chi_Minh,
   không cố định 20/11 hoặc clock prototype. Chỉ kiểm tra input cơ bản phía FE;
   thứ tự tuyến/phục vụ/ngày/cutoff/fare do BE quyết định, không hardcode A→B.
4. Search page=0, size=20; giữ metadata và điều khiển trang; giữ thứ tự server.
   Tiêu chí form đang sửa tách khỏi tiêu chí đã submit; ID chọn từ response.
5. Details gọi theo tripId và cặp Stop đã chọn, hỗ trợ không chọn cặp theo contract.
   Null ở route/vehicle/giờ/giá/description hiển thị rõ, không bịa dữ liệu.
   eligibility false vẫn xem được nhưng không sang Seats, gồm P4-01 inactive.
6. Seats theo đúng tripId/fromStopId/toStopId; layout theo ID và tọa độ, không suy ID.
   Hiển thị đủ deck, label, trạng thái bằng chữ và màu; expiry đọc được trên mobile.
   Đây là màn chỉ đọc, không tạo Booking/hold hoặc cho chọn/mua ghế.
7. Poll 15 giây khi tab hiển thị; refresh khi foreground, tới expiry/cutoff và bấm tay.
   Dùng serverTime để lập lịch tương đối, không tin giờ máy để quyết định ghế FREE.
   Không fetch dồn khi một request đang chạy; request cũ không ghi đè request mới.
   Timer hết hạn chỉ kích hoạt đọc lại. Refresh lỗi hoặc bản đồ quá hạn chưa xác minh:
   không hiển thị stale FREE như còn hiệu lực; hiển thị UNKNOWN/đang xác minh.
   409 cutoff/không thể tiếp tục thì chặn và hướng về chi tiết/tìm lại.
8. Mọi request chỉ được cập nhật màn nếu key chuyến/Segment/tiêu chí vẫn khớp.
   Response có Trip/Segment sai phải bị từ chối, không chỉ đổi nhãn hiển thị theo nó.
   Abort là hủy request, không hiện thành lỗi nghiệp vụ. Không fallback về demoData.
9. Gỡ preview công tắc loading/lỗi/hết ghế khỏi luồng người dùng; mocks chỉ trong tests.
   Không xóa test để CI xanh; thay tests prototype bằng kiểm thử hành vi API thật của FE.

## Xác minh và bàn giao

- Node 24.19.0 theo `.nvmrc`; `npm ci`, lint, typecheck, test, build thật.
- Unit/component tests dùng fetch mocks, fake timers và deferred promises;
  chúng không thay cho bằng chứng HTTP/UI thực tế.
- Runtime demo dùng Account→Trip→Booking runtime seed của main, DB riêng;
  kiểm tra A–B/A–C/B–C, giá/ghế, expiry, không phát sinh Booking và lỗi thật.
  Kịch bản #15 dùng DB riêng không có Booking; không nạp chồng lên DB demo ghép.
- Ảnh desktop/mobile bốn màn và mô tả mục đích/thông tin/cách dùng;
  handoff ghi thay đổi, file/luồng, cách chạy, tests, runtime đã/chưa chạy và giới hạn.
- Hoàng review rồi tiếp nhận; gửi phần FE đã xác minh cho Đức Hiếu cập nhật SAD.
  Không ghi hoàn thành trước khi có kết quả. Cảnh báo audit đã ghi nhận là task riêng,
  không thay dependency hoặc bỏ kiểm tra trong task nối API.
