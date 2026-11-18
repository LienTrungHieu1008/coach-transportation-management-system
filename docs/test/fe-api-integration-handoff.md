# Bàn giao FE — P04/P05/P07 nối API

Ngày bắt đầu: 18/11/2026. Nhánh `feat/pa4-journey-api-integration`, nền `5ce55ee`.
Trung Hiếu nhận hoàn thiện; Hoàng review/tiếp nhận, sau đó bổ sung nội dung FE cho
Đức Hiếu sửa SAD. Các owner khác đã gửi nội dung theo xác nhận của leader,
không yêu cầu gửi lại. Hoàng chưa review bản này; leader đã giao push nhánh và tạo PR để Hoàng review.

## Nguồn và phạm vi

- Tiếp nhận API client do Hoàng thêm ở PR #17 (`d556cd2`); sửa constructor TS1294,
  validation, paging metadata và optional Segment. Không nhận PROGRESS cũ ở gốc.
- Nối danh mục → tìm → chi tiết → ghế với bốn GET trên main sau #16.
- Giữ thiết kế F1, loại dữ liệu/công tắc prototype khỏi luồng chạy thật;
  ẩn Lịch trình theo quyết định leader.
- Không đổi BE/DTO/port/OpenAPI, migration, dependency ứng dụng, build/security/CI.
  Tool kiểm tra browser/PostgreSQL đặt ngoài repo; không thêm chúng vào package.json.
- Leader đã giao commit/push và tạo PR, yêu cầu Hoàng (`KirinVN1902`) review.
  Chưa merge; không sửa hoặc đóng #17 trong lượt bàn giao này.

## Luồng và một nhánh lỗi

`App` → `useJourneyState` → `journeyApi` → public HTTP → backend Trip/Booking.
Search giữ submitted criteria và metadata; chọn item giữ Trip/Stop IDs sang details.
Details được phép tiếp tục mới vào `useSeatAvailability`; GET không ghi DB.

Nếu refresh ghế lỗi hoặc deadline tới trước khi xác minh lại: hiển thị UNKNOWN,
không coi stale FREE còn hiệu lực. Timer kích hoạt GET; chỉ response mới đúng
Trip/Segment được cập nhật. Caller unmount/đổi lựa chọn hủy và vô hiệu response cũ.
P4 partial/inactive vẫn hiển thị facts, chặn bước ghế theo eligibility từ backend.

## Chạy trên môi trường nhóm

Từ gốc repo, theo `docs/development.md`:

```powershell
docker compose -f compose.yaml -f compose.dev.yaml up --build -d --wait db backend
```

Nạp fixture vào **DB demo riêng đã chỉ định**, theo
`src/backend/src/main/java/vn/edu/team11/coach/booking/README.md`: Account → Trip → Booking
trong một transaction, không nạp chồng bộ chuẩn/hết ghế. D là ngày mai VN lúc seed;
Hold B2 còn khoảng 2 phút. DB phát triển có dữ liệu không được reset tùy tiện.

Trong `src/frontend/`:

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
npm.cmd run dev
```

Chọn A/B/C và ngày D; thử A–B=100.000, A–C=250.000, B–C=150.000 VND.
Trên B–C, A2 Confirmed A–B phải FREE; B2 có thể đã hết hạn nếu thử muộn.
Theo dõi Network để thấy request thật; nút/nhãn giả lỗi đã được gỡ.

## Kiểm chứng

Đã chạy trên Windows, Node 24.19.0; `npm ci` thành công, không sửa lockfile.
Lượt kiểm tra cuối 18/11/2026: `npm run lint`, `npm run typecheck`, `npm test`,
`npm run build` đều exit 0. **5 test files, 41 tests PASS**: API client 13,
flow App 11, refresh ghế 6, request ownership/date 4, health 7.
`git diff --check` đạt. Unit/component mocks không tính thành Q3 functional.

`npm ci` vẫn báo 12 high trong chuỗi dev-tool dependencies đã ghi nhận;
không xử lý audit trong task FE, không đổi lockfile hoặc tắt kiểm tra.

### Browser + backend + PostgreSQL thật

Runtime ngày 18/11/2026: Spring Boot source main `5ce55ee`, Java 21.0.13,
PostgreSQL 16.16 portable và DB tổng hợp riêng `team11_fe_demo` trên loopback 55432.
Backend loopback 18081, FE Vite 5176 proxy tới backend này bằng harness ngoài source;
không đụng dịch vụ sẵn có ở 8080, không sửa Vite config/Compose của repo.

Docker daemon không chạy. Đã compile backend bằng Maven `test-compile`; plugin
`spring-boot:run -o` thiếu dependency cached nên không chạy được. Thay bằng Java
main thật với classpath dependencies đã được Maven resolve cho cùng pom, chỉ
nạp `target/classes` của bản này, không nạp test classes hoặc mock controller.
Không dùng backend giả. Không chạy lại full Maven verify/PostgreSQL suite ở lượt FE.

- Flyway V1–V4 áp dụng trên DB mới; Account → Trip → Booking seed atomic thành công.
- Browser Chrome headless gọi HTTP qua FE proxy, không intercept/mock API.
  Seed D=19/11/2026; A–B/A–C/B–C giá 100.000/250.000/150.000 và layout sáu ghế đúng.
- A2 Confirmed A–B trở lại FREE trên B–C; B2 còn Hold chuyển từ UNAVAILABLE sang
  FREE bằng refresh tự động sau expiry thật, không bấm làm mới/ép đồng hồ.
- Đã chụp đủ bốn màn desktop 1440px và mobile 390px; mobile không tràn ngang;
  đã đọc ảnh Home desktop, Results mobile, Details desktop và Seats mobile.
  Dark mode kiểm tra browser bổ sung; không có pageerror.
- Sau lượt bộ chuẩn, Booking/allocations vẫn 5/5 khi cleanup tắt, không tạo hold.
- Chỉ trên DB disposable trên, thay sạch dữ liệu Booking bằng fixture hết ghế:
  một transaction truncate ba bảng Booking rồi nạp fixture chính thức,
  không trộn allocations chuẩn và hết ghế; Account/Trip UUID giữ nguyên.
  Kết quả sáu UNAVAILABLE, không có FREE, HTTP thành công.
- Đặt Stop B inactive có kiểm soát rồi đọc lại P4: giữ giá A–C 250.000,
  eligibility false, nút tiếp tục disabled. Khôi phục Stop sau kiểm tra.
- Đổi tên bảng leg_fare tạm thời trên DB riêng để gây lỗi truy xuất thật:
  P4 HTTP 503 → UI báo lỗi, không fallback; khôi phục tên bảng → Thử lại thành công.
- B–C ngày D+1 trả đúng chuyến qua đêm lúc 00:30; ngày không có chuyến trả màn rỗng.
- Sau bộ hết ghế và các GET, Booking/allocations vẫn 1/6; thay đổi test dữ liệu
  ở trên là thao tác riêng của harness, không phải side effect của GET.

Scripts/logs/results runtime nằm trong `tmp/fe-integration/` được ignore;
ảnh sạch trong `docs/analysis and design/images/`. Không commit DB/build/logs/credentials.
Kiểm tra này không thay báo cáo Q3 do QA thực thi độc lập.

### Review code

Review độc lập phát hiện enum guard dùng String(...) có thể nhận status dạng array
và làm UI crash. Đã tái hiện test fail với guard cũ, sửa kiểm tra typeof string,
thêm regression cho seat status/Trip status/reasons. Client tests sau sửa đạt.
Reviewer không ghi nhận thêm Critical/Important trong phạm vi scan;
approval của Hoàng vẫn là bước nhận bàn giao riêng.

## Hoàng nhận bàn giao

1. Đọc diff client/state/pages và tài liệu `docs/analysis and design/pa4-ui-prototype.md`.
2. Chạy thử flow thật, kiểm tra request giữ đúng Segment, một nhánh lỗi và refresh.
3. Review PR của leader khi được chia sẻ; phản hồi rõ đã tiếp nhận/phần cần sửa.
4. Gửi Đức Hiếu paths và phần FE thay đổi đã xác minh để cập nhật SAD; ghi đúng
   phần Hoàng khởi tạo, leader hoàn thiện, Hoàng review. Không ghi Hoàng đã tự làm toàn bộ.

Chưa có login, tạo Booking/hold, thanh toán hoặc AI. Functional report Q3 do Đức Hiếu
thực thi/ghi nhận; tests/mock và ảnh giao diện không tự thay cho kết quả Q3.
