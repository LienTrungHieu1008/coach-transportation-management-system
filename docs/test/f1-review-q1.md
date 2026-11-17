# Review F1 — prototype bốn màn (PR #7), 17/11/2026

**Người review:** Nguyễn Đức Hiếu (QA, reviewer F1 theo [TASKS](../../pa/PA4/TASKS.md)) · **Tác giả phần được review:** Nguyễn Khánh Hoàng; PR #7 có thêm phần leader sửa tests/thiết kế.

**Bản được review:** `origin/main` tại `0115a8c` (PR #7 đã merge ngày 16/11). Phạm vi đọc:

- `src/frontend/src/App.tsx`
- `src/frontend/src/features/journey/JourneyPages.tsx`, `demoData.ts`, `useJourneyState.ts`, `JourneyIcon.tsx`
- `src/frontend/src/shared/api/journeyTypes.ts`
- `src/frontend/src/App.test.tsx`

**Cách review:** đọc mã nguồn và đối chiếu [UC Spec v1.8](../requirements/Team%2011%20-%20rup_ucspec-v2_final.pdf) (P04/P05/P07), [C1](../../pa/PA4/C1_DECISIONS.md), [OpenAPI 0.1](../contracts/openapi.yaml), [manifest dữ liệu demo P1](p1-trip-demo-manifest.md) và yêu cầu mục b của [đề PA4](../../pa/PA4/PA4.pdf).

**Chưa kiểm chứng trong lần review này:** chưa chạy `npm ci/lint/typecheck/test/build`, chưa mở trình duyệt, chưa kiểm tra trực quan desktop/mobile, chưa chạy backend. Kết quả CI của PR #7 do tác giả/leader báo, tôi không chạy lại. Nhận xét dưới đây là review mã nguồn và tài liệu, không phải kết quả thực thi.

---

## 1. Kết luận

F1 **đạt mục tiêu prototype luồng P04 → P05 → P07**: bốn màn có thật, điều hướng giữ được Segment, các nhánh lỗi/rỗng/thiếu dữ liệu đều có trạng thái riêng, và quan trọng nhất là **không chỗ nào biến dữ liệu chưa xác minh thành FREE hay giá 0**.

Còn **một thiếu sót cần xử lý trước khi nộp** (F1-01: chưa có tài liệu mô tả bốn màn — đây là phần đề PA4 chấm điểm) và một số điểm cần sửa/ghi nhận khi làm F2–F4.

| Mức | Số lượng | Ý nghĩa |
|---|---:|---|
| Cao | 1 | Ảnh hưởng điểm PA4 hoặc đánh giá sai năng lực sản phẩm; cần xử lý trước 20/11 |
| Trung bình | 5 | Phải xử lý khi nối API thật (F2–F4), nếu không sẽ sai contract hoặc gây hiểu nhầm |
| Thấp | 4 | Cải thiện chất lượng, không chặn |

## 2. Điểm làm tốt (giữ nguyên khi làm F2–F4)

1. **Không suy đoán trạng thái ghế.** `JourneyPages.tsx:154` lấy `statusBySeat.get(...)?.status ?? 'UNKNOWN'` — ghế thiếu dữ liệu rơi về UNKNOWN chứ không phải FREE. Nhánh lỗi availability hiển thị "Không có ghế nào được xem là trống" và ẩn toàn bộ sơ đồ. Đúng C1 mục 2 (Ghế) và UC-P07 A3.
2. **Điều kiện đi tiếp đúng.** `canContinue = allowed && segment !== null && segment.fareAmountVnd !== null` (`JourneyPages.tsx:133`) — khớp C1: eligibility không tự chứng minh đã chọn Segment, và thiếu giá thì không cho sang ghế.
3. **Phân biệt thiếu optional và thiếu essential.** Thiếu `description` vẫn xem được và vẫn cho đi tiếp; thiếu stops/legFares/vehicle thì chặn — đúng BR-P05-02.
4. **Giá lấy nguyên từ dữ liệu, không cộng ở client.** `money()` chỉ định dạng; không có phép cộng leg nào trong FE. Đúng C1 ("BE tính tổng leg").
5. **Ghi nhãn trung thực.** `DemoLabel`, footer và `demoData.ts:3` đều nói rõ đây là snapshot ví dụ, chưa nối API. Không giả là dữ liệu live.
6. **Giữ Segment khi điều hướng.** `useJourneyState` giữ `criteria` và `trip` qua các màn; test "preserves the journey through details, seats and back" bao phủ việc quay lại.
7. **Múi giờ.** Mọi chỗ hiển thị giờ/ngày đều đặt `timeZone: 'Asia/Ho_Chi_Minh'` (`JourneyPages.tsx:16–19`), đúng BR-P04-05.

## 3. Điểm cần xử lý

### F1-01 (Cao) — Chưa có tài liệu mô tả bốn màn hình

Đề PA4 mục b yêu cầu: với **mỗi màn hình** phải mô tả **mục đích, thông tin hiển thị và cách người dùng sử dụng**; đây là phần của 15 điểm prototype. Trong repo hiện chưa có tài liệu nào như vậy — `docs/test/f1-prototype-verification.md` là báo cáo sửa tests, không phải mô tả màn hình. TASKS cũng ghi đầu ra F1 là "mỗi màn có mục đích/thông tin/cách dùng và đường đi giữa màn".

**Đề nghị:** Hoàng bổ sung một tài liệu (gợi ý `docs/analysis and design/pa4-ui-prototype.md`) gồm ảnh chụp từng màn + ba đoạn mô tả, cho đúng bốn màn trong luồng: Trang chủ/tìm chuyến, Kết quả, Chi tiết chuyến, Sơ đồ ghế. Tôi cần tài liệu này để đưa vào hồ sơ Q2/gói nộp; hạn hợp lý là **18/11** để kịp tổng hợp trước 20/11 12h.

### F1-02 (Trung bình) — Màn "Lịch trình" dùng dữ liệu bịa, nằm ngoài ba UC

`JourneyPages.tsx:92–117`: `scheduleTrips` là bốn chuyến Sài Gòn–Nha Trang/Đà Lạt/Cần Thơ với giá và giờ tự đặt, hiển thị như một trang chức năng có trên thanh điều hướng chính.

Rủi ro: người chấm có thể hiểu đây là chức năng của hệ thống nhưng không có UC/API tương ứng; TASKS cũng yêu cầu không thêm chức năng hoạt động giả ngoài ba UC. Trang có ghi "dữ liệu giả lập" nhưng nhãn nằm cuối trang, dưới danh sách.

**Đề nghị:** bỏ trang này khỏi điều hướng chính trước khi demo/nộp, hoặc giữ lại nhưng đổi nhãn thành phần minh họa ngoài phạm vi PA4 và nêu ngay đầu trang. Quyết định cuối thuộc Hoàng + leader; tôi chỉ nêu rủi ro khi chấm.

### F1-03 (Trung bình) — Dữ liệu mẫu không khớp bộ demo chung của nhóm

`demoData.ts` lấy theo `docs/contracts/examples/` nên lệch với [manifest P1](p1-trip-demo-manifest.md) — là bộ dữ liệu cả nhóm đã thống nhất:

| Điểm | F1 hiện có | Manifest/C1 |
|---|---|---|
| Giá A→B | 120.000đ (`demoData.ts:18, 35, 42`) | 100.000đ |
| Số Stop | 2 (A, B) | 3 (A, B, C — `9999…9999`) |
| Số ghế | 2 (A1, A2) | 6 (A1, A2, B1, B2, C1, C2) |
| `seatLayoutId` | `5555…5555` | `bbbb…bbbb` |
| Giờ lên Trip ngày | 03:00Z (10:00 VN) | 02:00Z (09:00 VN) |

Hệ quả cho kiểm thử: không thể hiện được hai điểm then chốt của UC — **giá A→C = tổng hai leg** (BR-P04-03, ca TC-P04-02) và **chuyến qua đêm lên tại Stop giữa tuyến** (BR-P04-05, ca TC-P04-03). Prototype cũng chặn cứng việc chọn cặp Stop khác A→B (`JourneyPages.tsx:49`).

**Đề nghị:** khi làm F2–F4, lấy dữ liệu từ API thật theo bộ demo trong manifest; nếu còn cần snapshot cho tests FE thì cập nhật snapshot theo manifest (3 Stop, 6 ghế, 100k/150k) để QA và FE nói cùng một bộ dữ liệu.

### F1-04 (Trung bình) — Các nút "Preview nhánh UC-..." phải được gỡ trước khi demo/nộp

`JourneyPages.tsx:124, 136, 154` có khối `<details class="demo-controls">` cho phép bấm để ép trạng thái 503, "Trip đã đóng", "Hết ghế"… Với F1 thì hợp lý, nhưng khi FE nối API thật mà vẫn còn, người xem có thể nhầm là hệ thống thật và QA có thể vô tình ghi kết quả từ nút giả thay vì từ API. Tinh thần C1 là không để công tắc giả lỗi trong sản phẩm chạy thật.

**Đề nghị:** Hoàng gỡ (hoặc đặt sau cờ chỉ bật khi dev) trong F2–F4. Tôi sẽ không dùng các nút này làm bằng chứng cho bất kỳ ca nào ở Q3; ca 503 vẫn xác minh theo mức `IT` như đã ghi trong [ma trận ca](pa4-test-cases.md).

### F1-05 (Trung bình) — Màn chi tiết/ghế lấy tên Stop từ tiêu chí tìm kiếm, không từ response

`DetailsPage` và `SeatsPage` hiển thị `from`/`to` bằng cách tra `criteria.fromStopId/toStopId` trong `demoStops` (`JourneyPages.tsx:131–132, 143–144`), trong khi dữ liệu Segment lại nằm ở `detailState.selectedSegment` / `availability.fromStopId`. Hai nguồn này có thể lệch nhau khi người dùng đổi tiêu chí rồi quay lại, hoặc khi server trả Segment khác với yêu cầu.

**Đề nghị:** F3/F4 lấy Stop hiển thị từ chính response (`selectedSegment`, `stops`, `availability.fromStopId/toStopId`), chỉ dùng `criteria` để gửi request.

### F1-06 (Trung bình) — Chưa có cơ chế làm mới theo contract (sẽ làm ở F4)

`refresh()` (`JourneyPages.tsx:148`) chỉ phát lại snapshot khi người dùng bấm. C1 yêu cầu FE tự làm mới **mỗi 15 giây khi tab đang hiển thị, khi quay lại foreground và khi tới expiry/cutoff**, đồng thời response cũ không được ghi đè response mới.

Ghi nhận đây là việc của F4, không phải lỗi của F1; nêu ở đây để không bị bỏ sót — ca TC-P07-09 đang chờ phần này.

### F1-07 (Thấp) — Hạn giữ chỗ chỉ hiện trong tooltip

`expiresAt` được đưa vào thuộc tính `title` của ô ghế (`JourneyPages.tsx:154`). Trên thiết bị cảm ứng không có hover nên người dùng mobile không thấy được hạn giữ chỗ, dù đây là thông tin UC-P07 nhấn mạnh.

**Đề nghị:** hiện hạn giữ chỗ dạng chữ trong phần tóm tắt hoặc chú giải, không chỉ dựa vào tooltip.

### F1-08 (Thấp) — Ô ghế là `div` mang `aria-label`

Ô ghế không có `role`, nên trình đọc màn hình coi là phần tử chung; `aria-label` có thể bị bỏ qua tuỳ trình đọc. Gợi ý thêm `role="img"` (hoặc dựng dạng danh sách) để nhãn "Ghế A1: Còn trống" được đọc ổn định.

### F1-09 (Thấp) — Nhãn tầng xe suy từ ghế đầu tiên

`availability?.layout.seats[0]?.deck === 0 ? 'Tầng 1' : 'Tầng chưa xác định'` (`JourneyPages.tsx:154`) chỉ đúng với xe một tầng. Xe hai tầng sẽ hiển thị sai. Hiện chưa chặn gì vì dữ liệu demo một tầng; ghi nhận để xử lý nếu layout mở rộng.

### F1-10 (Thấp) — Số lượng kết quả đang là chuỗi cố định

`'1 chuyến trong ví dụ contract'` (`JourneyPages.tsx:122`) và việc chỉ render đúng một `demoTrip` khiến màn kết quả chưa thể hiện danh sách nhiều chuyến, thứ tự theo giờ lên tăng dần và phân trang (`page`/`size`) như OpenAPI quy định. Cần có ở F2 để chạy được ca TC-P04-01 (hai chuyến, đúng thứ tự).

## 4. Đối chiếu với yêu cầu đề PA4 mục b

| Yêu cầu | Tình trạng |
|---|---|
| Màn hình chính | Có (Trang chủ + form tìm chuyến) |
| Ít nhất 3 màn cho kịch bản chính | Có (Kết quả, Chi tiết, Sơ đồ ghế) |
| Khớp kịch bản trong UC spec | Có, luồng P04 → P05 → P07 đúng thứ tự; còn thiếu nhánh Segment A→C và chuyến qua đêm (F1-03) |
| Mô tả mục đích/thông tin/cách dùng từng màn | **Chưa có** (F1-01) |
| Đủ thành phần để người dùng hoàn thành việc của mình | Có cho luồng xem; không có nút đặt/thanh toán giả — đúng phạm vi |

## 5. Việc tôi đề nghị và người xử lý

| Mã | Người xử lý | Hạn đề nghị |
|---|---|---|
| F1-01 tài liệu mô tả bốn màn | Khánh Hoàng | 18/11 |
| F1-02 trang Lịch trình | Khánh Hoàng + leader quyết định | trước demo 19/11 |
| F1-03 đồng bộ dữ liệu theo manifest | Khánh Hoàng (phối hợp Minh Phương) | khi làm F2–F4 |
| F1-04 gỡ nút preview nhánh | Khánh Hoàng | trước demo 19/11 |
| F1-05, F1-06, F1-07 … F1-10 | Khánh Hoàng | trong F2–F4 |

Các điểm trên là nhận xét review, không phải lỗi đã kiểm chứng khi chạy. Sau khi F2–F4 nối API thật, tôi sẽ kiểm tra lại bằng 30 ca trong [ma trận ca Q1](pa4-test-cases.md) và ghi kết quả thực thi ở báo cáo Q3; những điểm trên nếu còn sẽ được ghi thành defect theo mẫu test-report.
