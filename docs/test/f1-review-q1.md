# Review F1 — prototype bốn màn (PR #7), 17/11/2026

**Người review:** Nguyễn Đức Hiếu (QA, reviewer F1 theo [TASKS](../../pa/PA4/TASKS.md)) · **Tác giả phần được review:** Nguyễn Khánh Hoàng; PR #7 có thêm phần leader sửa tests/thiết kế.

**Bản được review:** `origin/main` tại `0115a8c` (PR #7 đã merge ngày 16/11). Phạm vi đọc:

- `src/frontend/src/App.tsx`
- `src/frontend/src/features/journey/JourneyPages.tsx`, `demoData.ts`, `useJourneyState.ts`, `JourneyIcon.tsx`
- `src/frontend/src/shared/api/journeyTypes.ts`
- `src/frontend/src/App.test.tsx`

**Cập nhật 18/11:** sửa theo góp ý của leader trên PR #11 — tách nhận xét đọc mã với lỗi đã chạy (mục 1), tách việc còn lại của F2–F4 khỏi nhận xét thuộc F1 (mục 4 và 5), làm rõ giới hạn của bằng chứng mức IT (mục 6), và sửa lại nhận xét về nhãn trang "Lịch trình" cho đúng mã nguồn (R2). Mã cũ `F1-01…F1-10` được ánh xạ sang mã mới ở mục 8 để các bình luận đã viết trên PR vẫn tra được.

---

## 1. Tài liệu này là gì và không là gì

| | |
|---|---|
| **Là** | Nhận xét từ việc **đọc mã nguồn** và đối chiếu [UC Spec v1.8](../requirements/Team%2011%20-%20rup_ucspec-v2_final.pdf) (P04/P05/P07), [C1](../../pa/PA4/C1_DECISIONS.md), [OpenAPI 0.1](../contracts/openapi.yaml), [manifest P1](p1-trip-demo-manifest.md) và yêu cầu mục b của [đề PA4](../../pa/PA4/PA4.pdf). |
| **Không là** | Kết quả thực thi. **Không có mục nào trong tài liệu này là lỗi đã chạy ra.** |

Trong lần review này tôi **chưa chạy**: `npm ci`, `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`, chưa mở trình duyệt, chưa kiểm tra desktop/mobile, chưa chạy backend hay Compose. Kết quả CI của PR #7 do tác giả/leader báo; tôi không chạy lại và không trích dẫn như bằng chứng của mình.

Hệ quả cho cách dùng tài liệu:

- Mỗi mục dưới đây là **nhận xét đọc mã (R)** hoặc **việc còn lại đã biết (W)**, không phải defect đã tái hiện. Không mục nào được đưa vào bug report theo mẫu `test-report template-v1.1.xlsx` ở trạng thái hiện tại.
- Một nhận xét chỉ trở thành **defect** khi tôi chạy ca kiểm thử tương ứng ở Q3, ghi được steps + actual thật trên một commit cụ thể. Khi đó nó mới có Defect ID, severity và owner trong báo cáo.
- Nếu Hoàng hoặc chủ UC cho rằng tôi đọc sai mã hay sai phạm vi, phản hồi trên PR; tôi sửa tài liệu chứ không giữ nhận xét sai.

## 2. Kết luận

F1 **đạt mục tiêu prototype luồng P04 → P05 → P07**: bốn màn có thật, điều hướng giữ được Segment, các nhánh lỗi/rỗng/thiếu dữ liệu đều có trạng thái riêng, và quan trọng nhất là **không chỗ nào biến dữ liệu chưa xác minh thành FREE hay giá 0**.

Trong phạm vi F1 có **một việc cần xử lý trước khi nộp** (R1 — chưa có tài liệu mô tả bốn màn, là phần đề PA4 chấm điểm). Các điểm còn lại hoặc là rủi ro phạm vi cần leader quyết (R2), hoặc là **việc vốn thuộc F2–F4**, không tính là lỗi của F1.

## 3. Điểm F1 làm đúng (giữ nguyên khi làm F2–F4)

1. **Không suy đoán trạng thái ghế.** `JourneyPages.tsx:154` lấy `statusBySeat.get(...)?.status ?? 'UNKNOWN'` — ghế thiếu dữ liệu rơi về UNKNOWN chứ không phải FREE. Nhánh lỗi availability ẩn toàn bộ sơ đồ và ghi "Không có ghế nào được xem là trống". Đúng C1 mục 2 (Ghế) và UC-P07 A3.
2. **Điều kiện đi tiếp đúng.** `canContinue = allowed && segment !== null && segment.fareAmountVnd !== null` (`JourneyPages.tsx:133`) — khớp C1: eligibility không tự chứng minh đã chọn Segment, và thiếu giá thì không cho sang ghế.
3. **Phân biệt thiếu optional và thiếu essential.** Thiếu `description` vẫn xem được và vẫn cho đi tiếp; thiếu stops/legFares/vehicle thì chặn — đúng BR-P05-02.
4. **Giá lấy nguyên từ dữ liệu, không cộng ở client.** `money()` chỉ định dạng; không có phép cộng leg nào trong FE. Đúng C1 ("BE tính tổng leg").
5. **Ghi nhãn trung thực.** `DemoLabel`, footer và `demoData.ts:3` đều nói rõ đây là snapshot ví dụ, chưa nối API.
6. **Giữ Segment khi điều hướng.** `useJourneyState` giữ `criteria` và `trip` qua các màn; test "preserves the journey through details, seats and back" bao phủ việc quay lại.
7. **Múi giờ.** Mọi chỗ hiển thị giờ/ngày đều đặt `timeZone: 'Asia/Ho_Chi_Minh'` (`JourneyPages.tsx:16–19`), đúng BR-P04-05.

## 4. Nhận xét thuộc phạm vi F1

Đây là những điểm nằm trong đầu ra F1 theo TASKS ("bốn màn, mỗi màn có mục đích/thông tin/cách dùng và đường đi giữa màn").

### R1 (Cao) — Chưa có tài liệu mô tả bốn màn hình

Đề PA4 mục b yêu cầu với **mỗi màn hình** phải mô tả **mục đích, thông tin hiển thị và cách người dùng sử dụng**; đây là phần của 15 điểm prototype. Trong repo chưa có tài liệu nào như vậy — `docs/test/f1-prototype-verification.md` là báo cáo sửa tests, không phải mô tả màn hình. TASKS cũng ghi đây là đầu ra của F1.

**Đề nghị:** Hoàng bổ sung một tài liệu (gợi ý `docs/analysis and design/pa4-ui-prototype.md`) gồm ảnh chụp từng màn + ba đoạn mô tả, cho đúng bốn màn trong luồng: Trang chủ/tìm chuyến, Kết quả, Chi tiết chuyến, Sơ đồ ghế. Tôi cần tài liệu này cho hồ sơ Q2 và gói nộp; hạn hợp lý là **18/11**.

### R2 (Trung bình) — Trang "Lịch trình" nằm ngoài ba UC đã chốt

**Đính chính so với bản trước:** bản trước tôi viết nhãn minh hoạ "nằm cuối trang, dưới danh sách". Đọc lại mã thì **không đúng**. Trang đã ghi nhãn ở bốn chỗ: câu mô tả đầu trang "Tham khảo các chuyến xe đang có trong lịch mẫu" (`JourneyPages.tsx:103`), dòng ngay trên danh sách "Dữ liệu minh họa · Chưa kết nối cơ sở dữ liệu" (`:106`), nhãn "Theo lịch mẫu" trên **từng thẻ** (`:108`), và ghi chú cuối trang (`:112`). Việc ghi nhãn của Hoàng là đầy đủ và tôi rút lại nhận xét cũ.

Điểm còn lại không phải chuyện nhãn mà là **phạm vi**: `scheduleTrips` (`:92–97`) là bốn chuyến Sài Gòn–Nha Trang/Đà Lạt/Cần Thơ với tuyến, giờ và giá tự đặt, không thuộc UC-P04/P05/P07 và không có API/dữ liệu tương ứng, nhưng lại đứng ngang hàng với "Tìm chuyến" trên thanh điều hướng chính (`App.tsx:9`). Rủi ro khi chấm: người đọc có thể coi đây là một chức năng của hệ thống, trong khi phạm vi PA4 chỉ có ba UC.

**Đề nghị:** leader quyết định giữ hay bỏ trang này trước demo 19/11. Nếu giữ, cân nhắc đưa ra khỏi thanh điều hướng chính để không nằm trong luồng được chấm. Đây là quyết định phạm vi, không phải lỗi hiện thực.

### R3 (Thấp) — Hạn giữ chỗ chỉ hiện trong tooltip

`expiresAt` được đưa vào thuộc tính `title` của ô ghế (`JourneyPages.tsx:154`). Trên thiết bị cảm ứng không có hover nên người dùng mobile không thấy được hạn giữ chỗ, dù đây là thông tin UC-P07 nhấn mạnh. **Đề nghị:** hiện hạn giữ chỗ dạng chữ trong phần tóm tắt hoặc chú giải.

### R4 (Thấp) — Ô ghế là `div` mang `aria-label`

Ô ghế không có `role`, nên trình đọc màn hình coi là phần tử chung và `aria-label` có thể bị bỏ qua tuỳ trình đọc. Gợi ý thêm `role="img"` hoặc dựng dạng danh sách để nhãn "Ghế A1: Còn trống" được đọc ổn định.

### R5 (Thấp) — Nhãn tầng xe suy từ ghế đầu tiên

`availability?.layout.seats[0]?.deck === 0 ? 'Tầng 1' : 'Tầng chưa xác định'` (`JourneyPages.tsx:154`) chỉ đúng với xe một tầng. Dữ liệu demo hiện một tầng nên chưa sai; ghi nhận để xử lý nếu layout mở rộng.

## 5. Việc còn lại của F2–F4 — không tính là lỗi F1

Những điểm dưới đây **F1 không có nghĩa vụ làm**: theo TASKS, F1 là prototype bốn màn, còn nối API thật, refresh và danh sách kết quả thuộc F2/F3/F4. Ghi ở đây để không bị bỏ sót và để QA biết ca nào đang chờ phần nào.

| Mã | Việc | Vì sao cần | Ca kiểm thử đang chờ |
|---|---|---|---|
| W1 | Lấy tên Stop hiển thị từ response (`selectedSegment`, `stops`, `availability.fromStopId/toStopId`) thay vì tra từ `criteria` (`JourneyPages.tsx:131–132, 143–144`) | Khi server trả Segment khác yêu cầu, hoặc người dùng đổi tiêu chí rồi quay lại, hai nguồn có thể lệch nhau | TC-P05-01, TC-P07-01 |
| W2 | Làm mới theo contract: mỗi 15 giây khi tab hiển thị, khi quay lại foreground, khi tới expiry/cutoff; response cũ không ghi đè response mới. Hiện `refresh()` (`:148`) chỉ chạy khi bấm nút | C1 mục 2 (Refresh và cleanup) | TC-P07-09, TC-P07-03 |
| W3 | Màn kết quả render danh sách nhiều chuyến, sắp xếp theo giờ lên tăng dần, có `page`/`size`; hiện chỉ render một `demoTrip` và chuỗi cố định "1 chuyến trong ví dụ contract" (`:122`) | OpenAPI `/trips`; BR-P04-01 | TC-P04-01, TC-P04-06 |
| W4 | Cho chọn đủ ba Stop A/B/C để đi được Segment A→C và chuyến qua đêm; hiện form chặn mọi cặp khác A→B (`:49`) | BR-P04-03 (giá cộng leg), BR-P04-05 (ngày tại Stop lên) | TC-P04-02, TC-P04-03 |
| W5 | Gỡ các nút "Preview nhánh UC-…" (`:124, :136, :154`) hoặc đặt sau cờ chỉ bật khi dev, trước demo/nộp | Khi FE đã nối API thật mà vẫn còn, người xem dễ nhầm là hệ thống thật; tinh thần C1 là không để công tắc giả lỗi trong sản phẩm chạy thật | — (xem mục 6) |

### Về dữ liệu mẫu lệch manifest — không phải lỗi của F1

Bản trước tôi xếp việc này thành nhận xét F1. Đọc kỹ hơn thì `demoData.ts` **khớp đúng** `docs/contracts/examples/search.json` và `availability.json` như chú thích ở `demoData.ts:3` đã nói. Sai lệch nằm giữa **examples trong contract** và **manifest dữ liệu demo P1**:

| Điểm | examples (F1 dùng) | manifest P1 / C1 |
|---|---|---|
| Giá A→B | 120.000đ | 100.000đ |
| Số Stop | 2 (A, B) | 3 (A, B, C — `9999…9999`) |
| Số ghế | 2 (A1, A2) | 6 (A1, A2, B1, B2, C1, C2) |
| `seatLayoutId` | `5555…5555` | `bbbb…bbbb` |
| Giờ lên Trip ngày | 03:00Z (10:00 VN) | 02:00Z (09:00 VN) |

Hoàng dùng đúng nguồn được giao, nên đây **không phải lỗi F1**. Việc cần làm thuộc hai phía khác: Phương/leader quyết định có đồng bộ examples với manifest hay không, và F2–F4 khi nối API thật phải lấy dữ liệu từ server theo manifest chứ không dùng lại con số trong examples.

## 6. Bằng chứng mức IT không thay cho kết quả functional của Q3

Trong [ma trận ca Q1](pa4-test-cases.md) có 5 ca ghi mức `IT` (TC-P04-07, TC-P04-10, TC-P05-06, TC-P05-10, TC-P07-08). Lý do: theo C1, API public **không** được thêm công tắc giả lỗi, nên QA không tạo được tình huống 503 hay ép đồng hồ hệ thống từ bên ngoài.

Cách tôi sẽ ghi những ca này ở Q3, để không ai hiểu nhầm:

1. Đề PA4 yêu cầu **thực thi kiểm thử chức năng**. Kết quả chạy integration test của owner **không phải** kết quả functional và **không được** đánh dấu Pass cho ca chức năng.
2. Với các ca mức `IT`, báo cáo Q3 ghi trạng thái **"không thực thi được ở mức chức năng"** kèm lý do, rồi dẫn tên test, lệnh chạy và commit của owner như **thông tin bổ sung** về hành vi đã được kiểm chứng ở tầng khác.
3. Con số "đã thực thi" trong báo cáo chỉ đếm các ca chạy thật ở mức UI/API/DB. Hiện có 25 ca thuộc nhóm này, vẫn vượt mức tối thiểu 15 ca của đề.
4. Tương tự, các nút "Preview nhánh UC-…" của prototype (W5) **không** được dùng làm bằng chứng cho bất kỳ ca nào. Trạng thái lỗi ép bằng nút bấm trên FE không chứng minh backend trả đúng mã lỗi.

Khi bắt đầu Q3 tôi sẽ chỉnh lại mục 6 của [test plan](pa4-test-plan.md) theo đúng cách diễn đạt này, để định nghĩa mức `IT` trong plan không bị đọc thành "đã chạy".

## 7. Đối chiếu với yêu cầu đề PA4 mục b

| Yêu cầu | Tình trạng |
|---|---|
| Màn hình chính | Có (Trang chủ + form tìm chuyến) |
| Ít nhất 3 màn cho kịch bản chính | Có (Kết quả, Chi tiết, Sơ đồ ghế) |
| Khớp kịch bản trong UC spec | Có; luồng P04 → P05 → P07 đúng thứ tự. Segment A→C và chuyến qua đêm chưa đi được — thuộc W4 |
| Mô tả mục đích/thông tin/cách dùng từng màn | **Chưa có** (R1) |
| Đủ thành phần để người dùng hoàn thành việc của mình | Có cho luồng xem; không có nút đặt/thanh toán giả — đúng phạm vi |

## 8. Việc đề nghị, người xử lý và ánh xạ mã cũ

| Mã | Nội dung | Người xử lý | Hạn đề nghị | Mã ở bản 17/11 |
|---|---|---|---|---|
| R1 | Tài liệu mô tả bốn màn | Khánh Hoàng | 18/11 | F1-01 |
| R2 | Phạm vi trang "Lịch trình" | Leader quyết định, Hoàng thực hiện | trước demo 19/11 | F1-02 |
| R3–R5 | Tooltip hạn giữ chỗ, `role` ô ghế, nhãn tầng xe | Khánh Hoàng | trong F2–F4 | F1-07, F1-08, F1-09 |
| W1–W4 | Stop từ response, refresh theo contract, danh sách/sắp xếp/phân trang, đủ ba Stop | Khánh Hoàng | theo mốc F2–F4 (19/11) | F1-05, F1-06, F1-10, một phần F1-03 |
| W5 | Gỡ nút preview nhánh | Khánh Hoàng | trước demo 19/11 | F1-04 |
| — | Examples lệch manifest | Minh Phương + leader | khi rà contract | phần còn lại của F1-03 |

Sau khi F2–F4 nối API thật, tôi chạy 30 ca trong [ma trận ca Q1](pa4-test-cases.md) và ghi kết quả ở báo cáo Q3. Điểm nào còn tồn tại và tái hiện được khi chạy sẽ trở thành defect có steps, actual và commit cụ thể; trước lúc đó chúng vẫn chỉ là nhận xét đọc mã.
