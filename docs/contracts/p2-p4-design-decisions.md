# P2/P4 — ghi nhận quyết định thiết kế và kiểm thử

Ngày ghi nhận: **17/11/2026, giờ Việt Nam**. Owner P2/P4: **Đỗ Minh Phương**.
Hạn bàn giao P2/P4 theo Phương: **hết ngày 18/11/2026**.

## 1. Trạng thái và nguồn phê duyệt

Trong trao đổi với trợ lý, Phương thuật lại phản hồi của Trung Hiếu:
“Hãy làm theo đề xuất của bạn, note lại là được”. Sau đó Phương trực tiếp
chấp nhận đề xuất nhóm A và chốt các điểm nhóm B tại mục 4 dưới đây.
Đây là nguồn xác nhận trong hội thoại, không phải bằng chứng đã cập nhật C1
hoặc một approval implementation/PR trên GitHub.

Tài liệu này ghi nhận quyết định cho P2/P4, chưa chứng minh API hoặc tests đã
được hiện thực. Nhóm C vẫn chờ Đức Hiếu xác nhận. Việc tạo tài liệu không cấp
quyền sửa Q1, OpenAPI, C1, SAD, P3, security, migration hoặc workbook.

## 2. Căn cứ

| Nguồn | Phần sử dụng / giới hạn |
|---|---|
| [TASKS](../../pa/PA4/TASKS.md) | Mục Minh Phương: P2 danh mục/tìm chuyến, P4 chi tiết; ownership và reviewer. Deadline áp dụng theo chỉ dẫn trực tiếp của Phương nêu trên. |
| [C1](../../pa/PA4/C1_DECISIONS.md) | Mục 1–3: GET public, cutoff, search thiếu dữ liệu, partial details và ownership security/error mapping. |
| [OpenAPI 0.1](openapi.yaml) | `paths./stops`, `/trips`, `/trips/{tripId}`; parameters/responses; `MoneyVnd`, `SelectedSegment`, `BookingEligibility`, `TripDetails`, `ApiError`. Là nguồn schema HTTP, không tự mở rộng DTO. |
| [API decisions](pa4-api-decisions.md) | Input/lỗi, search thiếu dữ liệu, cặp Stop optional P05, paging và no-store. |
| [UC v1.8 — bản trích](../requirements/Team%2011%20-%20rup_ucspec-v2_final.extracted.md) | P04 trang PDF 10–12, BR-P04-01..05; P05 trang 12–13, BR-P05-01..03 và các nhánh A1–A4. |
| [SAD v1.3 — bản trích](../analysis%20and%20design/PA3_Group11_SAD_v1.3_final.extracted.md) | Trang PDF 24: ownership, TripStop, LegFare; trang 29, C4: ngày tại Stop lên và cutoff Stop đầu. Không sửa bản SAD gốc. |
| [Kiến trúc](architecture.md), [dữ liệu/API](data-and-api.md) | Module sở hữu dữ liệu; module khác dùng port; backend quyết định giá, thời gian và lỗi. |
| [TripQueryPort](trip-query-port.md) | Port P3, điều kiện thành công, lỗi có kiểu, mapping layout và transaction caller P07. |
| [Manifest P1](../test/p1-trip-demo-manifest.md), [V3](../../src/backend/src/main/resources/db/migration/V3__trip_read_model.sql) | UUID, Route/Stop/Vehicle/ghế/leg; V3 hiện có `route.is_active` và `stop.is_active`. Không sửa migration. |
| [Fixture cố định](../../src/backend/src/test/resources/fixtures/trip_demo.sql), [fixture runtime](../../src/backend/src/test/resources/fixtures/trip_demo_runtime.sql) | Tách dữ liệu kiểm thử lặp lại và demo dùng ngày tương đối. |
| Q1 tại commit `6b6730c44adf931260009251109cbe8ac538e062` | `docs/test/pa4-test-plan.md`, `pa4-test-cases.md`, `PA4-Group11-test-cases.xlsx`; đọc từ Git object database, không yêu cầu checkout. Bản v0.1 là thiết kế chưa thực thi, expected chưa được chủ UC rà tại snapshot đó. |
| Xác nhận của Phương ngày 17/11/2026 trong hội thoại | Căn cứ chấp nhận A, chốt served/timestamps/luồng P4, giữ P3/security và để C chờ Đức Hiếu. Không suy ra từ ngày sửa file Windows hoặc tài liệu tham khảo cũ. |

## 3. Nhóm A — đề xuất đã được Phương chấp nhận

- Chỉ triển khai `GET /api/v1/stops`, `GET /api/v1/trips` và
  `GET /api/v1/trips/{tripId}` trong P2/P4; không tạo Booking/hold hoặc tính ghế trống.
- Search dùng ngày tại Stop lên trong `Asia/Ho_Chi_Minh`, chỉ trả Scheduled
  còn bán. `bookingClosesAt` là giờ rời Stop đầu trừ 30 phút; đúng mốc là đóng.
- Giá Segment là tổng các leg liên tiếp; không thay giá thiếu bằng 0. Giá 0
  thực sự hợp lệ; tiền nguyên VND phải nằm trong giới hạn OpenAPI, không overflow.
- Candidate search phù hợp nhưng thiếu essential làm request trả
  `503 TRIP_DATA_UNAVAILABLE`, không âm thầm loại hoặc trả danh sách một phần.
- P4 trả dữ liệu đã xác minh khi Trip đóng/thiếu cấu hình: 200 và eligibility
  false. Trip không tồn tại là 404; lỗi truy xuất thật là 503.
- P4 không truyền cặp Stop thì `selectedSegment=null`, không chọn đoạn mặc định.
  Không chọn Segment không tự làm eligibility false. Cặp đã xác minh sai là 422;
  thiếu một tham số trong cặp là 400.
- Trả đủ eligibility reasons đã xác định, không lặp, theo thứ tự:
  `TRIP_NOT_SCHEDULED`, `BOOKING_CLOSED`, `ESSENTIAL_DATA_INCOMPLETE`.
  Không thêm `BOOKING_CLOSED` nếu chưa xác định được cutoff.
- Giữ required fields và nullable theo schema; UTC kết thúc `Z`, no-store cho
  response thành công/lỗi. Không lộ SQL/stack hoặc thêm field ngoài contract.
- Bổ sung coverage catalogue, paging, duplicate scalar, format và biên thời gian/giá;
  phân biệt unit, controller, PostgreSQL IT, HTTP IT và FE/UI.

Đây là chấp nhận nội dung thiết kế/expected. Cách chia Case ID, bố trí workbook
và ghi bằng chứng Q1/Q3 vẫn thuộc nhóm C, chưa cho phép sửa các file đó.

## 4. Nhóm B — các điểm Phương đã chốt

### 4.1. Stop served

Một Stop được phục vụ khi đồng thời:

1. Stop active.
2. Thuộc ít nhất một Route.
3. Route đó active nếu schema có trạng thái active của Route.

V3 hiện có cả hai cột active nên áp dụng cả hai. Không phụ thuộc việc hiện tại
có Trip đang mở bán. Danh mục không chứng minh mọi cặp Stop đều có thể đi cùng
nhau: search vẫn kiểm tra một Route phục vụ cặp theo đúng thứ tự.
Không thêm cột hoặc thay schema.

Căn cứ: Phương chốt trực tiếp; cụ thể hóa từ “served” trong OpenAPI. Các ca mới
cần phân biệt Stop không tồn tại, inactive, không thuộc Route, chỉ thuộc Route
inactive và thuộc Route active nhưng không có Trip còn bán.

### 4.2. Intermediate Stop times

- Boarding dùng `plannedDepartureTime`; alighting dùng `plannedArrivalTime`.
- Thiếu timestamp tương ứng: P4 trả null tại field liên quan và đánh dấu
  `ESSENTIAL_DATA_INCOMPLETE`; không fallback sang timestamp còn lại.
- Nếu xác minh được Stop và thứ tự thì vẫn trả `selectedSegment`, kể cả khi
  giờ/giá tương ứng chưa đủ. Chỉ trả segment null khi không đủ căn cứ xác minh,
  hoặc khi request không chọn cặp Stop theo OpenAPI.
- Đối với P2, response search không cho phép giờ item null: nếu candidate phù hợp
  thiếu essential thì áp dụng 503 theo C1/OpenAPI, không trả item chứa giờ null.
- Không tự dựng timestamp từ Schedule. Không coi arrival null tại Stop đầu hoặc
  departure null tại Stop cuối trong fixture là lỗi chỉ vì field đó null; phải xét
  timestamp tương ứng với boarding/alighting và các điều kiện essential thực tế.

Căn cứ: Phương chốt trực tiếp; `SelectedSegment`/`TripStop` cho phép timestamp
null, còn `TripSearchItem` yêu cầu timestamp không null. Quyết định này không
cho phép thay đổi validation hoặc contract P3.

### 4.3. Luồng đọc riêng P4 và phần chung

P4 có luồng đọc riêng trong module Trip để hỗ trợ partial details, không phụ
thuộc hoàn toàn vào `TripQueryPort`. Giữ nguyên OpenAPI, C1, SAD và P3 contract.
Không sửa `SecurityConfiguration`; Trung Hiếu/C2 phụ trách security và phối hợp
error mapping chung. Việc public GET đã được contract xác định, nhưng vẫn cần
ghép C2 trước nghiệm thu HTTP anonymous.

## 5. P4 partial details, P3 và Booking

### 5.1. Ranh giới TripQueryPort và Booking Application Service

P3 cung cấp facts thuộc Trip qua `TripQueryPort`, không sở hữu allocations hoặc
điều phối toàn bộ P07. Theo mục Giao diện của [contract port](trip-query-port.md),
facts chỉ thành công khi Scheduled, `asOf < bookingClosesAt`, Segment hợp lệ và
đủ essential. [Adapter hiện tại](../../src/backend/src/main/java/vn/edu/team11/coach/trip/infrastructure/PostgresTripQueryAdapter.java)
có kiểm tra status/cutoff theo `asOf` và giá từng leg trên toàn Trip. Đây là
trách nhiệm cụ thể đã có trong contract/implementation P3, không phải khẳng định
port tự kiểm tra toàn bộ điều kiện bán/đặt chỗ hoặc bảo đảm còn ghế.

Booking Application Service P07/H2 của Huy chịu trách nhiệm điều phối request:
lấy `asOf` có thẩm quyền, mở transaction/snapshot theo contract P07, gọi port,
đọc allocations thuộc Booking, xét trạng thái/expiry/overlap và ánh xạ kết quả,
lỗi sang HTTP P07. Port tham gia transaction caller; `MANDATORY` của adapter
không tự mở transaction hoặc đặt isolation. Booking không đọc repository/entity
Trip, và P3 không tính trạng thái ghế từ allocations thay Booking.

`TripSeatFacts` không chứa toàn bộ chi tiết HTTP và port có thể từ chối facts
khi P4 vẫn phải cho xem thông tin. P4 không chỉ gọi port rồi bắt exception để
dựng dữ liệu không có trong kết quả port; không nới contract/implementation P3.

### 5.2. Cùng nguồn dữ liệu và quy tắc completeness toàn Trip

Luồng đọc riêng P4 là một cách trình bày khác của cùng dữ liệu Trip hiện hành:
dùng các bảng V3 thuộc Trip, cùng UUID, thứ tự Stop, TripStop, Vehicle/layout và
LegFare. Không tạo bảng/bản sao có thẩm quyền, nguồn dữ liệu hoặc bộ business
rule độc lập. Các quy tắc dùng chung vẫn truy vết về UC/C1/OpenAPI và các quyết
định đã ghi nhận ở đây; khác biệt giữa P3 từ chối facts và P4 trả partial details
là cách phản hồi theo use case, không phải hai định nghĩa giá hoặc cutoff.
Không refactor P3 trong lần chỉnh tài liệu này và không trả entity qua HTTP.

P4 không tạo business rules độc lập với P3. Ưu tiên tái sử dụng domain policy
hiện có nếu phù hợp mà không refactor P3. Nếu chưa thể tái sử dụng, phải có
regression tests đối chiếu cùng dữ liệu/thời điểm để chứng minh cùng quy tắc
cutoff, fare và completeness; phân biệt quy tắc chung với cách phản hồi riêng
của P3/P4 và các khác biệt tích hợp đã ghi ở mục 5.4. Đây là yêu cầu kiểm thử
cho implementation sau, chưa phải tests đã viết hoặc đã chạy.

P4 phải kiểm tra completeness của LegFare trên **toàn bộ Trip**: mọi cặp Stop
liên tiếp trên toàn Trip cần có giá cấu hình hợp lệ, không chỉ các leg trong
Segment được chọn. Fare âm là invalid; fare bằng 0 là hợp lệ và không được coi
là missing. Kiểm tra từng leg và tổng giá theo giới hạn tiền của OpenAPI.
Ví dụ thiếu B–C nhưng hỏi A–B: vẫn trả Segment và giá A–B 100.000 đã xác minh;
`bookingEligibility.allowed=false`, reasons chứa `ESSENTIAL_DATA_INCOMPLETE`.
Nếu hỏi A–C thì giá Segment null vì thiếu leg trong đoạn đó. Không thay giá thiếu
bằng 0, không loại mất giá hợp lệ ngoài phần dữ liệu thiếu.

Phân biệt hai kết quả đọc:

- **Missing configuration:** truy vấn hoàn tất nhưng bản ghi/cấu hình bắt buộc
  thiếu hoặc không hợp lệ; P4 trả 200 với dữ liệu xác minh được, null/collection
  theo schema và eligibility false. Không suy ra thiếu cấu hình chỉ từ exception DB.
- **Database access failure:** truy vấn không thể hoàn tất hoặc không đọc/xác minh
  được facts do lỗi truy cập/hạ tầng DB; trả `503 TRIP_DATA_UNAVAILABLE`, không
  biến lỗi đọc thành bản ghi vắng hoặc response 200 một phần.

Trip không tồn tại sau truy vấn thành công là 404; cặp Stop xác định được là sai
là 422. Không dùng missing configuration hoặc partial details để che hai lỗi này.

### 5.3. Nhất quán khi đọc P4

Toàn bộ phần đọc Trip/Route/Stop/TripStop/Vehicle/layout/LegFare và tính response
P4 phải dùng một transaction read-only phù hợp với snapshot nhất quán, cùng
DataSource/transaction manager. Không đọc từng phần trong các transaction độc
lập khiến status, Stop order, giờ và fare thuộc các trạng thái dữ liệu khác nhau.
Chỉ gắn `readOnly` không tự bảo đảm snapshot xuyên nhiều câu SELECT.

Với cách triển khai nhiều SELECT trên PostgreSQL, đề xuất dùng `REPEATABLE_READ`
cho transaction application P4; đây là lựa chọn implementation P4, không sửa
transaction P3 hoặc biến quy định transaction P07 thành yêu cầu đã có cho P4.
P4 phải capture duy nhất một `Instant asOf` cho mỗi request từ clock server có
thẩm quyền; ưu tiên injected `Clock` để kiểm thử thời gian xác định. Dùng cùng
`asOf` cho mọi phép kiểm tra eligibility và giá trị `serverTime` trong response;
không lấy lại thời gian ở từng bước hoặc từng field. Không khóa/giữ ghế hoặc mở
transaction ghi Booking từ GET chi tiết.

Cần PostgreSQL IT với reader P4 và writer ở transaction/connection khác:
writer commit thay đổi Trip/Stop/Fare giữa các bước đọc; response reader phải
nhất quán với snapshot của nó, transaction mới thấy cập nhật đã commit. Không
dùng sleep để suy đoán thứ tự và không coi SnapshotIT P3 là bằng chứng P4 đã đạt.

### 5.4. Lưu ý tích hợp giữ nguyên P3/Booking

P3 hiện có kiểm tra served dựa trên Stop active và fallback arrival/departure
để kiểm tra chronology. Không suy ra các kiểm tra đó đã đáp ứng mọi quyết định
P2/P4 mới chốt. Cần ghi nhận cho Phương/Huy/Trung Hiếu khi rà tích hợp; không tự
sửa adapter, DTO hoặc tests P3 để đồng bộ trong task này.

Booking/H2 tiếp tục chỉ dùng `trip.contract`, không đọc repository/entity Trip.
P4 eligibility không chứng minh còn ghế hoặc bảo đảm lần gọi P07 sẽ thành công.
Snapshot test P3 hiện có không chứng minh snapshot xuyên Trip–Booking; việc đó
thuộc tích hợp H2. P2/P4 không cần V4 để kiểm thử dữ liệu Trip riêng.

## 6. Test cases tương ứng — nội dung đề xuất, chưa thực thi

Tên subcase trong bảng là nhãn mô tả, chưa phải Case ID Q1 chính thức.

| Nhóm / ca Q1 liên quan | Tiền điều kiện và expected cần có |
|---|---|
| P04-03: ngày tại Stop giữa | Fixture cố định D=20/11/2026; clock D 08:00 VN. B→C ngày D trả Trip ngày, ngày D+1 trả overnight; cả hai giá 150.000. |
| P04-03: cutoff overnight | Tìm B→C ngày D+1 tại D 21:59:59.999, 22:00:00.000 và 22:00:00.001 VN. Overnight chỉ xuất hiện trước cutoff; còn lại 200/items rỗng nếu không còn candidate khác. Không đổi cutoff thành giờ B trừ 30 phút. |
| P05-05: status reason | Cancelled, trước cutoff, đủ essential → 200/false và chỉ `TRIP_NOT_SCHEDULED`. |
| P05-05: nhiều reasons | Cancelled + hết cutoff + đủ essential → status rồi closed; Cancelled + trước cutoff + thiếu fare → status rồi essential; cả ba điều kiện → đủ ba reasons đúng thứ tự. Giữ đủ dữ liệu để xác định cutoff trong các ca cần closed. |
| P05-07: fare thuộc Segment thiếu | Xóa fare B→C trong DB test, hỏi A→C, Stop/giờ đủ → 200; Segment có ID/giờ, giá null; leg thiếu vắng, essential incomplete. |
| P05-07: fare ngoài Segment thiếu | Cùng thiếu B→C, hỏi A→B → giá vẫn 100.000 nhưng eligibility false do completeness toàn chuyến. |
| P05-07: layout thiếu | Stop/giờ/fare đủ, cấu hình ghế thiếu → giữ Segment/giá xác minh được, essential incomplete. Fixture phải tuân thủ V3, không gán null trái NOT NULL của `seat_layout_id`. |
| P05-07: Segment chưa xác minh | Thiếu bản ghi làm không đủ căn cứ xác minh Stop/thứ tự → segment null, essential incomplete; đối chiếu ca B→A có dữ liệu đủ phải 422. |
| P05-10 và phần lỗi DB P04-10 | Lỗi truy xuất thật → 503 `TRIP_DATA_UNAVAILABLE`; không dùng thiếu cấu hình thay cho lỗi DB, không thêm công tắc giả lỗi public. |
| Timestamp theo quyết định B | Tại B, chỉ có arrival nhưng thiếu departure: boardingTime null; chỉ có departure nhưng thiếu arrival: alightingTime null. P4 vẫn giữ Segment nếu xác minh được thứ tự và eligibility false; không fallback. Candidate P2 thiếu essential không được trả item giờ null. |
| GET /stops và served | Catalogue có dữ liệu/rỗng; sort name/ID; chỉ Stop active thuộc Route active; không cần Trip còn bán. Kiểm tra Stop không Route/inactive và cặp không có Route đúng thứ tự. |
| Paging / sort | Mặc định 0/20; size 1/100; trang vượt cuối rỗng nhưng tổng thật; tie-break tripId; không trùng/mất qua trang; page/size ngoài giới hạn → 400. |
| Duplicate / format | Lặp từng scalar đã khai báo, kể cả cùng giá trị → 400; UUID/date sai, input bắt buộc thiếu, hai chiều cặp optional thiếu một bên → 400. |
| Giá / ranh giới ngày | A→B=100.000, B→C=150.000, A→C=250.000; giá 0 hợp lệ; giới hạn tiền theo schema. Ngày quá khứ xét theo clock server ở Việt Nam tại biên 00:00. |
| P05-02/06/08 và schema | Không chọn Segment không tự đổi eligibility; đúng cutoff đóng; optional description null không chặn khi mọi điều kiện khác đạt; required/null/UTC Z/no-store đúng contract. |
| Hồi quy điểm ghép P3/P4 | Với fixture thiếu fare/layout mà P3 hiện từ chối: giữ hành vi P3, P4 trả partial details. Không áp expected P2/P4 mới cho P3 khi chưa có task/duyệt riêng. |

Đối với lỗi dữ liệu vượt giới hạn tiền, chỉ chốt ràng buộc schema và không
overflow; không tự sáng tạo mapping lỗi HTTP chưa được contract xác định.

### Dữ liệu và clock đưa vào workbook khi được duyệt

| Định danh | Giá trị |
|---|---|
| Stop A | `22222222-2222-4222-8222-222222222222` |
| Stop B | `33333333-3333-4333-8333-333333333333` |
| Stop C | `99999999-9999-4999-8999-999999999999` |
| Trip ngày | `11111111-1111-4111-8111-111111111111` |
| Overnight | `11111111-1111-4111-8111-111111111112` |
| Route | `44444444-4444-4444-8444-444444444444` |
| Cutoff Trip ngày | `2026-11-20T01:30:00Z` = D 08:30 VN |
| Cutoff overnight | `2026-11-20T15:00:00Z` = D 22:00 VN |
| Boarding B overnight | `2026-11-20T17:30:00Z` = D+1 00:30 VN |

Ghi fixture/commit, D cụ thể, clock UTC và giờ VN, trạng thái/completeness,
UUID thật, DB test riêng và expected candidates. IT dùng clock cố định, không
đổi giờ hệ điều hành. Runtime seed dùng ngày tương đối phải ghi ngày thực tế.
Không gửi literal A/B/C như UUID. Bổ sung tiền điều kiện bị thiếu tại workbook
hàng 4, 5, 14, 18–21 khi Đức Hiếu xác nhận cách chỉnh.

## 7. Phân tầng và bằng chứng kiểm thử

| Tầng | Kiểm chứng | Giới hạn |
|---|---|---|
| Unit | Cutoff, ngày VN, tổng fare, reasons/completeness trên facts đầu vào | Không chứng minh SQL, HTTP hoặc security. |
| Controller | Binding/duplicate/format, status/error body, schema/null/header | Mock persistence không chứng minh PostgreSQL. |
| PostgreSQL IT | Join/order, served, ngày Stop lên, fare coverage, paging/count và fixtures incomplete | Gọi service không chứng minh endpoint anonymous hoặc UI. |
| HTTP + PostgreSQL IT | Endpoint tới DB thật; JSON/status/header; public GET sau ghép C2 | Không chứng minh giao diện. |
| FE/UI | Hiển thị null/giá/lỗi, giữ Segment, loading/rỗng/retry, nút đi tiếp | Không thay bằng chứng nghiệp vụ backend/DB. |

Mock lỗi repository chỉ chứng minh mapping. Ca tuyên bố kiểm chứng lỗi
PostgreSQL phải có lỗi DB có kiểm soát trong môi trường test riêng. Không lấy
Maven xanh hoặc tests P3 cũ làm kết quả P2/P4 hay toàn bộ ca UI.

## 8. Nhóm C — chờ Đức Hiếu xác nhận

- Cách tách/đánh số Case ID, giữ liên kết P04-03/P05-05/P05-07/P05-10 cũ và
  cập nhật ma trận truy vết, số lượng ca.
- Vị trí tiền điều kiện/UUID/fixture/clock trong workbook; đồng bộ Markdown–Excel.
- Expected hiển thị Route dùng đúng `route.id/name` của fixture; `DEMO-ABC`
  là route code, không tự thêm field API để khớp một nhãn expected.
- Phân entry criteria: P2/P4 API/DB riêng dùng V1–V3; flow toàn bộ có Booking
  cần V4/C2. Không bắt mọi test P2/P4 chờ FE/Booking.
- Cách gắn Case ID với unit/controller/IT/UI; test name, commit, môi trường,
  dữ liệu, Actual/Status/Tester/Date và bằng chứng Q3. Làm rõ cách dùng ca mức IT
  mà không suy diễn các assertion UI đã pass.
- Chỉnh tiền điều kiện cho expected một reason, bỏ expected “segment null hoặc
  giá null” chung chung; đổi tên ca optional description để không ngụ ý đã có
  chức năng tạo Booking hoặc bảo đảm ghế.

Nội dung expected A/B đã được Phương chấp nhận; nhóm C là phối hợp tài liệu và
ghi bằng chứng, chưa có xác nhận của Đức Hiếu, không được ghi thay approval.

## 9. Phạm vi lần bàn giao này

Chỉ thêm file Markdown này. Không sửa Q1, OpenAPI, C1, SAD, source production,
P3, Booking, migration, dependency, security, cấu hình chung hoặc workbook.
Không chạy build/tests/Docker, không commit/push/merge hoặc đổi nhánh.
Các ca ở đây là thiết kế/đề xuất, **chưa thực thi**, không đánh dấu pass.
