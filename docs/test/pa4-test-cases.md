# PA4 — Ma trận ca kiểm thử (Test Cases) Nhóm 11

**Task:** Q1 · **Người soạn:** Nguyễn Đức Hiếu · **Ngày:** 17/11/2026 · **Phiên bản:** 0.1 (bản đầu)

Đi kèm [kế hoạch kiểm thử](pa4-test-plan.md). Bản theo mẫu Moodle: `docs/test/PA4-Group11-test-cases.xlsx`.

**Chưa ca nào được thực thi.** Cột *Actual Result*, *Status*, *Tester*, *Tested Date* trong file Excel để trống và sẽ được điền ở task Q3. Expected Result dưới đây **chưa được chủ UC rà soát**.

## Quy ước

| Cột | Ý nghĩa |
|---|---|
| Ưu tiên | `P1` = luồng chính và quy tắc nghiệp vụ bắt buộc, chạy trước; `P2` = nhánh phụ/lỗi, chạy sau |
| Mức | `UI`, `UI+API`, `API`, `DB`, `IT` — xem mục 6 của test plan |
| Bộ dữ liệu | `demo` = seed demo chuẩn (`trip_demo_runtime.sql` + fixture Booking H1); các bộ khác do owner cung cấp riêng |
| Owner | Người sửa khi ca này Fail |

Ký hiệu dữ liệu: **D** = ngày khởi hành của Trip demo (ngày mai tại thời điểm seed, giờ `Asia/Ho_Chi_Minh`). Stop A/B/C, Trip ngày/qua đêm, 6 ghế và UUID theo [manifest P1](p1-trip-demo-manifest.md); ghế 1–6 là A1, A2, B1, B2, C1, C2 theo thứ tự layout.

Trạng thái ghế fixture (C1): ghế 1 trống · ghế 2 Confirmed A–B · ghế 3 Confirmed A–C · ghế 4 hold B–C còn hiệu lực · ghế 5 hold đã hết hạn · ghế 6 allocation đã huỷ/giải phóng.

Mọi ca đều yêu cầu response có `Cache-Control: no-store` và **không** tạo Booking/hold.

---

## UC-P04 — Tìm chuyến (10 ca)

### TC-P04-01 — Tìm chuyến A→B ngày D trả đúng danh sách

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương (dữ liệu/API), Khánh Hoàng (hiển thị)
- **Tiền điều kiện:** bộ `demo` đã seed; thời điểm chạy trước 08:30 ngày D; không đăng nhập.
- **Dữ liệu:** Stop lên = A, Stop xuống = B, ngày = D.
- **Các bước:**
  1. Mở `http://localhost:8088`.
  2. Chọn Stop lên "Demo Stop A" từ danh sách Stop lấy qua `GET /api/v1/stops`.
  3. Chọn Stop xuống "Demo Stop B".
  4. Nhập ngày đi = D.
  5. Bấm "Tìm chuyến".
- **Kết quả mong đợi:**
  1. HTTP 200, hiển thị đúng **2 chuyến**: Trip ngày (lên 09:00, xuống 11:00 ngày D) và Trip qua đêm (lên 22:30 ngày D, xuống 00:30 ngày D+1).
  2. Thứ tự: giờ lên tăng dần (09:00 trước 22:30).
  3. Mỗi chuyến hiển thị tuyến DEMO-ABC, Stop A→B, loại xe "Demo coach", giá **100.000đ**, hạn đặt (08:30 ngày D cho Trip ngày; 22:00 ngày D cho Trip qua đêm).
  4. Không có chuyến nào ngoài bộ demo; không có giá/giờ trống hoặc "0".
- **Nguồn:** UC-P04 Basic Flow 1–4; BR-P04-01, BR-P04-04; OpenAPI `/trips`.

### TC-P04-02 — Giá Segment A→C bằng tổng hai leg

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương
- **Tiền điều kiện:** như TC-P04-01.
- **Dữ liệu:** A → C, ngày D.
- **Các bước:** tìm chuyến với Stop lên A, Stop xuống C, ngày D.
- **Kết quả mong đợi:**
  1. 200 với 2 chuyến; giá mỗi chuyến là **250.000đ** (100.000 + 150.000).
  2. Giá trong response `fareAmountVnd = 250000` (số nguyên VND); FE hiển thị đúng giá trị server trả, không tự cộng ở client.
  3. Giờ lên/xuống là giờ tại Stop A và Stop C.
- **Nguồn:** BR-P04-03; C1 mục 2 (BE tính tổng leg).

### TC-P04-03 — Chuyến qua đêm lên tại Stop giữa tuyến tính theo ngày tại Stop lên

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương
- **Tiền điều kiện:** bộ `demo`; chạy trước 22:00 ngày D.
- **Dữ liệu:** B → C, ngày D+1; sau đó B → C, ngày D.
- **Các bước:**
  1. Tìm B → C với ngày = **D+1**.
  2. Tìm lại B → C với ngày = **D**.
- **Kết quả mong đợi:**
  1. Bước 1: trả **Trip qua đêm** (lên tại B lúc 00:30 ngày D+1, xuống C 02:30 ngày D+1), giá 150.000đ, hạn đặt vẫn **22:00 ngày D** (cutoff tính theo Stop đầu tuyến, không đổi theo Stop giữa).
  2. Bước 1 **không** trả Trip ngày.
  3. Bước 2: trả **Trip ngày** (lên tại B lúc 11:00 ngày D); **không** trả Trip qua đêm.
- **Nguồn:** BR-P04-05 (ngày = ngày tại Stop lên, giờ `Asia/Ho_Chi_Minh`), BR-P04-02.

### TC-P04-04 — Ngày đi trong quá khứ bị từ chối

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Khánh Hoàng (UI), Minh Phương (API)
- **Tiền điều kiện:** bộ `demo`.
- **Dữ liệu:** A → B, ngày = hôm qua (giờ VN).
- **Các bước:**
  1. Nhập ngày hôm qua trên form và bấm tìm.
  2. Gọi trực tiếp `GET /api/v1/trips?fromStopId=A&toStopId=B&boardingDate=<hôm qua>`.
- **Kết quả mong đợi:**
  1. UI báo lỗi ngày không hợp lệ, giữ nguyên tiêu chí để sửa, không hiển thị danh sách kết quả.
  2. API trả **422** với `code = PAST_BOARDING_DATE`.
  3. Không có Booking/allocation nào được tạo.
- **Nguồn:** UC-P04 A1; BR-P04-05; OpenAPI `InvalidCriteria`.

### TC-P04-05 — Cặp Stop không hợp lệ hoặc không được phục vụ

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương
- **Tiền điều kiện:** bộ `demo`.
- **Dữ liệu:** (a) A → A; (b) C → A; (c) Stop UUID hợp lệ nhưng không thuộc tuyến nào.
- **Các bước:** lần lượt gửi ba yêu cầu tìm chuyến với ngày D.
- **Kết quả mong đợi:**
  1. (a) và (b): **422**, `code = INVALID_SEGMENT`, thông điệp nêu Stop lên phải trước Stop xuống.
  2. (c): **422**, `code = STOP_NOT_SERVED`.
  3. UI hiển thị lỗi tương ứng và cho sửa tiêu chí; không hiển thị kết quả rỗng như thể hợp lệ.
- **Nguồn:** UC-P04 A1; OpenAPI `InvalidCriteria`.

### TC-P04-06 — Không có chuyến phù hợp trả kết quả rỗng, không phải lỗi

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương, Khánh Hoàng
- **Tiền điều kiện:** bộ `demo` (chỉ có chuyến ngày D).
- **Dữ liệu:** A → B, ngày = D+5.
- **Các bước:** tìm chuyến với ngày D+5.
- **Kết quả mong đợi:**
  1. HTTP **200**, `items` rỗng, `totalElements = 0`.
  2. UI hiển thị thông báo không có chuyến phù hợp và cho phép sửa tiêu chí.
  3. Không hiển thị chuyến của ngày khác, không hiện thông báo lỗi hệ thống.
- **Nguồn:** UC-P04 A2; OpenAPI `search-empty.json`.

### TC-P04-07 — Cutoff 30 phút: đúng mốc là đóng

- **Ưu tiên/Mức:** P1 · IT (+UI nếu seed được) · **Owner:** Minh Phương
- **Tiền điều kiện:** test dùng Clock cố định của owner, hoặc seed dữ liệu có giờ phù hợp trên DB demo.
- **Dữ liệu:** Trip ngày, cutoff 08:30 ngày D.
- **Các bước:**
  1. Với thời điểm hệ thống 08:29 ngày D, tìm A → B ngày D.
  2. Với thời điểm 08:30 ngày D, tìm lại.
  3. Với thời điểm 08:31 ngày D, tìm lại.
- **Kết quả mong đợi:**
  1. 08:29: Trip ngày **có** trong kết quả.
  2. 08:30 và 08:31: Trip ngày **không** còn trong kết quả (đúng mốc đã đóng).
  3. Trip qua đêm (cutoff 22:00) vẫn còn ở cả ba thời điểm.
- **Nguồn:** BR-P04-02; C1 mục 2 ("đúng mốc là đóng").

### TC-P04-08 — Chuyến không ở trạng thái bán không xuất hiện

- **Ưu tiên/Mức:** P2 · UI+API · **Owner:** Minh Phương
- **Tiền điều kiện:** nạp bộ `trip-not-sellable` (Cancelled / Departed / Completed, và một Trip Scheduled đã qua cutoff) trên DB demo riêng.
- **Dữ liệu:** Segment và ngày trùng với các Trip trong bộ này.
- **Các bước:** tìm chuyến theo Segment/ngày tương ứng.
- **Kết quả mong đợi:**
  1. Không chuyến nào trong bốn trạng thái trên xuất hiện trong kết quả.
  2. Nếu không còn chuyến nào khác: 200 với `items` rỗng, không lỗi.
- **Nguồn:** BR-P04-01, BR-P04-02.

### TC-P04-09 — Tham số sai định dạng trả 400

- **Ưu tiên/Mức:** P2 · API · **Owner:** Minh Phương
- **Tiền điều kiện:** backend đang chạy.
- **Dữ liệu:** (a) thiếu `boardingDate`; (b) `fromStopId=abc` (không phải UUID); (c) `size=101`.
- **Các bước:** gọi `GET /api/v1/trips` lần lượt với ba bộ tham số.
- **Kết quả mong đợi:**
  1. Cả ba trả **400**, `code = INVALID_REQUEST`.
  2. `fieldErrors` chỉ đúng tham số sai; thông điệp không chứa SQL, stack trace hay thông tin nội bộ.
- **Nguồn:** OpenAPI `InvalidRequest`; C1 mục 2 (400 format/cấu trúc).

### TC-P04-10 — Không truy xuất được dữ liệu Trip thì báo lỗi, không bịa kết quả

- **Ưu tiên/Mức:** P2 · IT · **Owner:** Minh Phương
- **Tiền điều kiện:** theo C1 **không** thêm công tắc giả lỗi vào API public; ca này xác minh qua integration test có kiểm soát của owner (ứng viên thiếu cấu hình bắt buộc hoặc lỗi truy xuất).
- **Các bước:**
  1. QA chạy lại lệnh verify backend của owner trên commit được kiểm thử.
  2. Đọc tên test tương ứng và kết quả.
- **Kết quả mong đợi:**
  1. Trường hợp ứng viên phù hợp nhưng thiếu cấu hình bắt buộc: **503**, `code = TRIP_DATA_UNAVAILABLE`.
  2. Không trả danh sách một phần và không âm thầm loại chuyến khỏi kết quả.
  3. Trên UI (nếu tái hiện được): hiển thị lỗi tải và nút thử lại, không hiển thị danh sách cũ như kết quả mới.
- **Nguồn:** UC-P04 A3; C1 mục 2 (search có dữ liệu hỏng → 503).

---

## UC-P05 — Xem chi tiết chuyến (10 ca)

### TC-P05-01 — Chi tiết chuyến với Segment A→C hiển thị đầy đủ và cho phép đi tiếp

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương, Khánh Hoàng
- **Tiền điều kiện:** bộ `demo`; chạy trước 08:30 ngày D; đã tìm chuyến theo TC-P04-02.
- **Dữ liệu:** Trip ngày, from = A, to = C.
- **Các bước:**
  1. Từ kết quả tìm kiếm, chọn Trip ngày.
  2. Quan sát màn chi tiết và response `GET /api/v1/trips/{tripId}?fromStopId=A&toStopId=C`.
- **Kết quả mong đợi:**
  1. 200; `status = Scheduled`.
  2. Danh sách Stop theo thứ tự index 0 (A), 1 (B), 2 (C) với giờ 09:00 / 11:00 / 13:00 ngày D.
  3. `legFares` có đúng **hai** dòng A–B 100.000 và B–C 150.000; **không** có dòng A–C.
  4. `selectedSegment.fareAmountVnd = 250000`; `bookingClosesAt` = 08:30 ngày D.
  5. `bookingEligibility.allowed = true`, `reasons` rỗng; nút xem ghế trống bật.
- **Nguồn:** UC-P05 Basic Flow; BR-P05-01, BR-P05-03.

### TC-P05-02 — Không chọn Segment thì không bịa giá

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương, Khánh Hoàng
- **Tiền điều kiện:** bộ `demo`.
- **Dữ liệu:** Trip ngày, không truyền `fromStopId`/`toStopId`.
- **Các bước:** gọi `GET /api/v1/trips/{tripId}` không kèm cặp Stop; mở màn chi tiết khi chưa chọn Segment.
- **Kết quả mong đợi:**
  1. 200 với `selectedSegment = null`; vẫn có Stop, giờ, `legFares`.
  2. UI không hiển thị giá Segment nào và yêu cầu chọn Stop lên/xuống trước khi xem ghế.
  3. Không hiển thị giá mặc định, 0đ hay giá của Segment khác.
- **Nguồn:** OpenAPI `/trips/{tripId}`; C1 mục 2 (Details).

### TC-P05-03 — Chỉ truyền một Stop trong cặp bị từ chối

- **Ưu tiên/Mức:** P2 · API · **Owner:** Minh Phương
- **Dữ liệu:** `?fromStopId=A` (thiếu `toStopId`).
- **Các bước:** gọi `GET /api/v1/trips/{tripId}?fromStopId=A`.
- **Kết quả mong đợi:** **400**, `code = INVALID_REQUEST`, `fieldErrors` nêu cặp Stop phải cùng có hoặc cùng vắng.
- **Nguồn:** OpenAPI `/trips/{tripId}` (optional only together).

### TC-P05-04 — Trip không tồn tại trả 404 và đưa người dùng về tìm kiếm

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương, Khánh Hoàng
- **Dữ liệu:** `tripId` là UUID hợp lệ không có trong DB.
- **Các bước:** mở URL chi tiết với tripId đó.
- **Kết quả mong đợi:**
  1. **404**, `code = TRIP_NOT_FOUND`.
  2. UI thông báo chuyến không còn khả dụng và cho quay lại tìm kiếm.
  3. Không hiển thị dữ liệu chuyến khác hoặc dữ liệu đã cache.
- **Nguồn:** UC-P05 A1.

### TC-P05-05 — Trip đã huỷ vẫn xem được nhưng không cho đặt

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương
- **Tiền điều kiện:** bộ `trip-not-sellable`.
- **Dữ liệu:** Trip trạng thái Cancelled.
- **Các bước:** mở chi tiết Trip Cancelled.
- **Kết quả mong đợi:**
  1. **200**, `status = Cancelled`, vẫn hiển thị thông tin xác minh được.
  2. `bookingEligibility.allowed = false`, `reasons = [TRIP_NOT_SCHEDULED]`.
  3. UI hiển thị trạng thái hiện tại và vô hiệu hoá đường đi sang xem ghế/đặt chỗ.
- **Nguồn:** UC-P05 A2; BR-P04-01; C1 mục 2 (Details).

### TC-P05-06 — Sau cutoff thì đóng bán nhưng vẫn xem được thông tin

- **Ưu tiên/Mức:** P2 · IT hoặc UI · **Owner:** Minh Phương
- **Tiền điều kiện:** Clock cố định tại/sau 08:30 ngày D, hoặc seed Trip đã qua cutoff.
- **Các bước:** mở chi tiết Trip ngày tại thời điểm ≥ cutoff.
- **Kết quả mong đợi:**
  1. 200; `status = Scheduled`; `allowed = false`, `reasons = [BOOKING_CLOSED]`.
  2. Thông tin Stop/giờ/giá vẫn hiển thị đúng.
  3. UI không cho tiếp tục sang chọn ghế.
- **Nguồn:** UC-P05 A2; BR-P04-02.

### TC-P05-07 — Thiếu dữ liệu essential: chặn đặt, không điền giá thay

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Minh Phương
- **Tiền điều kiện:** bộ `incomplete-trip` (thiếu một leg fare hoặc thiếu layout xe).
- **Các bước:** mở chi tiết Trip trong bộ này, có truyền cặp Stop hợp lệ.
- **Kết quả mong đợi:**
  1. **200** (không phải 503) với dữ liệu xác minh được.
  2. `allowed = false`, `reasons` chứa `ESSENTIAL_DATA_INCOMPLETE`.
  3. Leg thiếu giá **vắng mặt** trong `legFares`, **không** xuất hiện dòng giá 0; `selectedSegment` là `null` hoặc `fareAmountVnd = null`.
  4. UI nêu rõ không thể đặt do thiếu dữ liệu, không hiển thị 0đ như giá thật.
- **Nguồn:** UC-P05 A3; BR-P05-01, BR-P05-02; C1 mục 2.

### TC-P05-08 — Thiếu mô tả optional không chặn việc xem và đặt

- **Ưu tiên/Mức:** P2 · UI+API · **Owner:** Minh Phương, Khánh Hoàng
- **Tiền điều kiện:** Trip demo hợp lệ nhưng `description = null`.
- **Các bước:** mở chi tiết Trip đó với Segment hợp lệ.
- **Kết quả mong đợi:**
  1. 200; `description = null`; UI đánh dấu "không có mô tả" thay vì để trống khó hiểu.
  2. `allowed = true`, `reasons` rỗng; vẫn đi tiếp sang xem ghế được.
- **Nguồn:** BR-P05-02.

### TC-P05-09 — Segment không hợp lệ trên chi tiết trả 422

- **Ưu tiên/Mức:** P2 · API · **Owner:** Minh Phương
- **Dữ liệu:** Trip ngày với `fromStopId = B`, `toStopId = A`.
- **Các bước:** gọi `GET /api/v1/trips/{tripId}?fromStopId=B&toStopId=A`.
- **Kết quả mong đợi:** **422**, `code = INVALID_SEGMENT`; không trả `selectedSegment` suy đoán.
- **Nguồn:** OpenAPI `/trips/{tripId}`; C1 mục 2.

### TC-P05-10 — Không truy xuất được chi tiết thì báo lỗi, không hiển thị dữ liệu bịa

- **Ưu tiên/Mức:** P2 · IT · **Owner:** Minh Phương
- **Tiền điều kiện:** xác minh qua integration test lỗi hạ tầng có kiểm soát của owner (không thêm công tắc giả lỗi vào API public).
- **Các bước:** QA chạy lại verify backend trên commit kiểm thử và đọc kết quả test tương ứng.
- **Kết quả mong đợi:**
  1. **503**, `code = TRIP_DATA_UNAVAILABLE`, không phải 200 một phần.
  2. Không trả chi tiết suy đoán và không cho đặt chỗ với dữ liệu chưa xác minh.
- **Nguồn:** UC-P05 A4; C1 mục 2.

---

## UC-P07 — Xem ghế trống theo Segment (10 ca)

### TC-P07-01 — Sơ đồ ghế Segment A→B đúng quy tắc chặn

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Nhật Huy, Khánh Hoàng (hiển thị)
- **Tiền điều kiện:** bộ `demo` + fixture ghế H1; trước cutoff.
- **Dữ liệu:** Trip ngày, from = A, to = B.
- **Các bước:**
  1. Từ màn chi tiết (TC-P05-01) chọn "Xem ghế trống" với Segment A→B.
  2. Đối chiếu response `GET /api/v1/trips/{tripId}/seat-availability?fromStopId=A&toStopId=B`.
- **Kết quả mong đợi:**
  1. 200; `layout.seats` đủ **6 ghế** với đúng `physicalSeatId`, nhãn A1…C2 và toạ độ deck/row/column theo manifest.
  2. `seats` tương ứng **1-1** với `layout.seats`, cùng thứ tự, không thừa/thiếu/trùng ID.
  3. Ghế 2 (Confirmed A–B) và ghế 3 (Confirmed A–C) = `UNAVAILABLE`.
  4. Ghế 1, 4, 5, 6 = `FREE` với `expiresAt = null` (hold B–C của ghế 4 không chặn A–B).
  5. `bookingClosesAt` = 08:30 ngày D; `serverTime` là giờ server.
- **Nguồn:** UC-P07 Basic Flow 1–5; BR-P07-02; OpenAPI `/seat-availability`.

### TC-P07-02 — Segment B→C: đoạn kề không giao nhau, hold hiển thị expiry

- **Ưu tiên/Mức:** P1 · UI+API · **Owner:** Nhật Huy
- **Tiền điều kiện:** như TC-P07-01; hold của ghế 4 còn hiệu lực.
- **Dữ liệu:** Trip ngày, from = B, to = C.
- **Các bước:** xem ghế với Segment B→C.
- **Kết quả mong đợi:**
  1. Ghế 2 (Confirmed A–B) = `FREE` — A→B và B→C không giao nhau.
  2. Ghế 3 (Confirmed A–C) = `UNAVAILABLE`, `expiresAt = null`.
  3. Ghế 4 (hold B–C còn hiệu lực) = `UNAVAILABLE` với `expiresAt` đúng bằng hạn hold.
  4. Ghế 5 (hold đã hết hạn) và ghế 6 (đã huỷ/giải phóng) = `FREE`.
  5. Ghế 1 = `FREE`.
- **Nguồn:** BR-P07-01, BR-P07-02; C1 mục 2 (Hold).

### TC-P07-03 — Hold hết hạn trong lúc xem thì ghế được giải phóng

- **Ưu tiên/Mức:** P1 · UI+API+DB · **Owner:** Nhật Huy
- **Tiền điều kiện:** bộ `hold-expiring` (hold tạo lúc `seedTime−8 phút`, hết hạn `seedTime+2 phút`), job dọn hold đang chạy.
- **Dữ liệu:** Trip ngày, Segment B→C, ghế có hold sắp hết hạn.
- **Các bước:**
  1. Mở sơ đồ ghế ngay sau khi seed, ghi nhận trạng thái ghế và `expiresAt`.
  2. Giữ tab hiển thị, chờ qua mốc `expiresAt`.
  3. Quan sát lần refresh kế tiếp (hoặc bấm làm mới).
  4. Truy vấn đọc trạng thái Booking tương ứng trong DB demo.
- **Kết quả mong đợi:**
  1. Trước hạn: ghế `UNAVAILABLE` kèm `expiresAt`.
  2. Sau hạn: ghế chuyển `FREE` mà người dùng không cần tải lại trang thủ công.
  3. Booking Pending tương ứng chuyển trạng thái `Expired` do job dọn, không phụ thuộc việc có người mở trang.
  4. Việc xem **không** giữ/đặt ghế cho người đang xem.
- **Nguồn:** UC-P07 A4; BR-P07-01, BR-P07-03, BR-P07-04; C1 mục 2 (Refresh và cleanup).

### TC-P07-04 — Hết ghế vẫn là kết quả bình thường

- **Ưu tiên/Mức:** P2 · UI+API · **Owner:** Nhật Huy, Khánh Hoàng
- **Tiền điều kiện:** nạp bộ `all-seats-unavailable` cho Segment kiểm thử.
- **Các bước:** xem ghế trên Segment đó.
- **Kết quả mong đợi:**
  1. HTTP **200** (không phải lỗi); cả 6 ghế `UNAVAILABLE`.
  2. UI nêu rõ không còn ghế trống và gợi ý chọn chuyến/đoạn khác.
  3. Không hiển thị ghế nào là `FREE`.
- **Nguồn:** UC-P07 A2; C1 mục 2 (Ghế).

### TC-P07-05 — Trip/Segment không hợp lệ trên API ghế

- **Ưu tiên/Mức:** P2 · API · **Owner:** Nhật Huy
- **Dữ liệu:** (a) `fromStopId = B`, `toStopId = A`; (b) `tripId` không tồn tại; (c) thiếu `fromStopId`.
- **Các bước:** gọi `/seat-availability` lần lượt với ba bộ tham số.
- **Kết quả mong đợi:**
  1. (a) **422** `INVALID_SEGMENT`; (b) **404** `TRIP_NOT_FOUND`; (c) **400** `INVALID_REQUEST`.
  2. Không trả sơ đồ ghế một phần trong cả ba trường hợp.
- **Nguồn:** UC-P07 A1; OpenAPI `/seat-availability`.

### TC-P07-06 — Chuyến không còn bán thì không cho xem sơ đồ ghế

- **Ưu tiên/Mức:** P2 · UI+API · **Owner:** Nhật Huy
- **Tiền điều kiện:** bộ `trip-not-sellable` (Trip Cancelled; và Trip Scheduled đã qua cutoff).
- **Các bước:** gọi `/seat-availability` cho từng Trip trên với Segment hợp lệ.
- **Kết quả mong đợi:**
  1. **409**, `code = TRIP_NOT_BOOKABLE` cho cả hai trường hợp.
  2. UI không hiển thị sơ đồ ghế, nêu lý do và mời chọn chuyến/đoạn khác.
- **Nguồn:** UC-P07 A1; BR-P04-01, BR-P04-02; C1 mục 2 (Lỗi).

### TC-P07-07 — Thiếu cấu hình layout bắt buộc trả 409, không đoán ghế

- **Ưu tiên/Mức:** P2 · API · **Owner:** Nhật Huy (phối hợp Minh Phương về dữ liệu Trip)
- **Tiền điều kiện:** bộ `incomplete-trip` thiếu layout/ghế của xe.
- **Các bước:** gọi `/seat-availability` cho Trip đó.
- **Kết quả mong đợi:**
  1. **409**, `code = ESSENTIAL_DATA_INCOMPLETE`.
  2. Không sinh ghế giả, không suy ID ghế từ nhãn hay toạ độ.
- **Nguồn:** OpenAPI `TripNotBookable`; C1 mục 2 (Layout).

### TC-P07-08 — Không xác minh được dữ liệu thì hiển thị UNKNOWN, không coi là trống

- **Ưu tiên/Mức:** P1 · IT (+UI) · **Owner:** Nhật Huy, Khánh Hoàng
- **Tiền điều kiện:** xác minh qua integration test lỗi hạ tầng có kiểm soát; phía FE có thể kiểm tra bằng cách chặn/làm hỏng response trong DevTools.
- **Các bước:**
  1. QA chạy lại verify backend trên commit kiểm thử, đọc test tương ứng.
  2. Trên UI: mô phỏng lỗi mạng khi refresh sơ đồ ghế.
- **Kết quả mong đợi:**
  1. API trả **503**, `code = AVAILABILITY_UNAVAILABLE`.
  2. UI hiển thị trạng thái `UNKNOWN`/không xác định và thông báo thử lại.
  3. **Không** ghế nào bị chuyển từ `UNAVAILABLE`/`UNKNOWN` thành `FREE` do lỗi tải.
- **Nguồn:** UC-P07 A3; C1 mục 2 (Ghế, Refresh).

### TC-P07-09 — Làm mới định kỳ và chống ghi đè bằng response cũ

- **Ưu tiên/Mức:** P2 · UI · **Owner:** Khánh Hoàng
- **Tiền điều kiện:** bộ `demo`; mở DevTools tab Network.
- **Các bước:**
  1. Mở sơ đồ ghế, giữ tab hiển thị trong ~1 phút và đếm số lần gọi `/seat-availability`.
  2. Chuyển sang tab khác rồi quay lại (foreground).
  3. Làm chậm một request (throttling) để response cũ về sau response mới.
- **Kết quả mong đợi:**
  1. Có gọi lại khoảng mỗi 15 giây khi tab đang hiển thị.
  2. Khi quay lại foreground, gọi lại ngay.
  3. Response cũ đến muộn **không** ghi đè kết quả mới hiển thị.
- **Nguồn:** C1 mục 2 (Refresh và cleanup); BR-P07-03.

### TC-P07-10 — Xem ghế không tạo Booking/hold và không cho phép cache

- **Ưu tiên/Mức:** P1 · UI+DB · **Owner:** Nhật Huy
- **Tiền điều kiện:** bộ `demo`; quyền truy vấn đọc DB demo.
- **Các bước:**
  1. Đếm số bản ghi Booking, BookingSeat, SeatAllocation trước khi xem.
  2. Mở sơ đồ ghế, để refresh vài lần, chuyển qua lại giữa chi tiết và ghế.
  3. Đếm lại các bản ghi trên.
  4. Kiểm tra header của các response liên quan.
- **Kết quả mong đợi:**
  1. Số bản ghi **không đổi** ở cả ba bảng; không có hold mới cho người xem.
  2. Mọi response có `Cache-Control: no-store`.
  3. Giữ nguyên Segment khi quay lại màn chi tiết rồi vào lại sơ đồ ghế.
- **Nguồn:** BR-P07-04; C1 mục 2 (Lỗi/cache); UC-P07 Postconditions.

---

## Ma trận truy vết UC/BR → ca kiểm thử

| Nguồn yêu cầu | Ca kiểm thử |
|---|---|
| UC-P04 Basic Flow | TC-P04-01, TC-P04-02, TC-P04-03 |
| UC-P04 A1 (tiêu chí sai) | TC-P04-04, TC-P04-05, TC-P04-09 |
| UC-P04 A2 (không có chuyến) | TC-P04-06, TC-P04-08 |
| UC-P04 A3 (không truy xuất được) | TC-P04-10 |
| BR-P04-01 | TC-P04-01, TC-P04-08, TC-P05-05, TC-P07-06 |
| BR-P04-02 | TC-P04-03, TC-P04-07, TC-P05-06, TC-P07-06 |
| BR-P04-03 | TC-P04-02, TC-P05-01 |
| BR-P04-04 | TC-P04-01, TC-P05-07 |
| BR-P04-05 | TC-P04-03, TC-P04-04 |
| UC-P05 Basic Flow | TC-P05-01, TC-P05-02 |
| UC-P05 A1 | TC-P05-04 |
| UC-P05 A2 | TC-P05-05, TC-P05-06 |
| UC-P05 A3 | TC-P05-07 |
| UC-P05 A4 | TC-P05-10 |
| BR-P05-01 | TC-P05-01, TC-P05-07 |
| BR-P05-02 | TC-P05-07, TC-P05-08 |
| BR-P05-03 | TC-P05-01, TC-P04-02 |
| UC-P07 Basic Flow | TC-P07-01, TC-P07-02 |
| UC-P07 A1 | TC-P07-05, TC-P07-06, TC-P07-07 |
| UC-P07 A2 | TC-P07-04 |
| UC-P07 A3 | TC-P07-08 |
| UC-P07 A4 | TC-P07-03 |
| BR-P07-01 | TC-P07-02, TC-P07-03 |
| BR-P07-02 | TC-P07-01, TC-P07-02 |
| BR-P07-03 | TC-P07-03, TC-P07-09 |
| BR-P07-04 | TC-P07-03, TC-P07-10 |

## Phụ thuộc chưa sẵn sàng tại 17/11

| Cần | Cho ca | Ai cung cấp |
|---|---|---|
| API `/stops`, `/trips` (P2) và `/trips/{tripId}` (P4) | toàn bộ P04, P05 | Minh Phương |
| API `/seat-availability` (H2) và job dọn hold (H3) | toàn bộ P07 | Nhật Huy |
| FE nối API thật (F2–F4) | các ca mức UI | Khánh Hoàng |
| Bộ `trip-not-sellable`, `incomplete-trip` | TC-P04-08, TC-P05-05/06/07, TC-P07-06/07 | Minh Phương |
| Bộ `all-seats-unavailable`, `hold-expiring` | TC-P07-03, TC-P07-04 | Nhật Huy |
| Tên test và lệnh chạy cho các ca mức `IT` | TC-P04-07, TC-P04-10, TC-P05-06, TC-P05-10, TC-P07-08 | Minh Phương, Nhật Huy |
| Môi trường tích hợp (C2) có GET public truy cập được | mọi ca mức UI/API | Liên Trung Hiếu |
