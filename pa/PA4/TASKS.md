# Task PA4 giao cho thành viên — cập nhật 14/11/2026

Phạm vi đã chốt: P04/P05/P07. **Trung Hiếu đã duyệt C1 kỹ thuật và phân công dưới đây ngày 14/11/2026**; từng thành viên cần phản hồi nhận việc và lịch dự kiến. C1 là quyết định kỹ thuật đã chốt; các task hiện thực còn **chưa nghiệm thu**. Mốc là mục tiêu nội bộ, không phải kết quả đã hoàn thành. Đọc [handbook](../../docs/team-handbook.md) để biết cách tạo branch, push, PR và review.

**Tiến độ I1 ngày 14/11:** đã viết V2, fixture và tests; full backend verify đạt 43 tests,0 skipped. [Contract Account cho Huy](../../docs/contracts/identity-account.md) và [báo cáo kiểm chứng](../../docs/test/i1-account-verification.md) đã có. Chờ Hoàng review/merge trước khi tính I1 nghiệm thu hoặc V2 có trên main; các task còn lại chưa được đánh dấu xong.

## Đọc mã task và migration cho đúng

**Migration là file SQL mô tả một lần thay đổi cấu trúc database**, ví dụ tạo bảng, thêm cột, index hoặc khóa ngoại (FK: ràng buộc bản ghi tham chiếu phải tồn tại). File được commit cùng source để máy của mọi người tạo/nâng cấp DB theo cùng lịch sử. Flyway trong backend tìm migration chưa chạy, áp dụng theo version và ghi lịch sử vào `flyway_schema_history`; nó không chạy lại mọi file mỗi lần mở app.

| Mã / file | Nghĩa cụ thể | Ai làm / tình trạng |
|---|---|---|
| `V1__spring_session.sql` | Thay đổi DB số 1: tạo hai bảng lưu phiên và thuộc tính phiên của Spring Session, kèm index/FK. Không phải bảng Account. | Đã có trong baseline; giữ nguyên. |
| `V2__identity_account.sql` | Thay đổi số 2: tạo cấu trúc Account/role tối thiểu để Booking tham chiếu đúng người dùng. | Trung Hiếu, task I1; đã viết/test, chờ review/merge. |
| `V3__trip_read_model.sql` | Thay đổi số 3: tạo cấu trúc tuyến/Stop/chuyến/xe/ghế/giá từng leg theo SAD. Đây là dữ liệu Trip có thẩm quyền, không nhân bản một bộ Trip thứ hai. | Phương, task P1; chưa viết. |
| `V4__booking_availability.sql` | Thay đổi số 4: tạo cấu trúc Booking/ghế trong Booking/phân bổ ghế để tính availability và expiry. | Huy, task H1; chưa viết. |

`V2` là **version migration**; `I1/P1/H1` là **mã việc được giao**; `UC-P04` là **mã yêu cầu nghiệp vụ**. Chúng không cùng một hệ đánh số. Trong tên file, `V2` là số thứ tự, `__` ngăn cách với mô tả, `.sql` là phần SQL thực thi.

Schema migration tạo cấu trúc, còn **seed/fixture** nạp dữ liệu mẫu để demo/test; hai việc tách riêng. Không gửi file dump DB cá nhân thay migration. Không sửa migration đã chia sẻ/áp dụng; cần đổi thì xin version mới và thêm file mới. Không bật skip/out-of-order hoặc xóa dữ liệu để làm mất lỗi migration.

## Việc chung trước khi code riêng

Quyết định giao việc: [C1_DECISIONS.md](C1_DECISIONS.md). Dùng [OpenAPI 0.1](../../docs/contracts/openapi.yaml) và [TripQueryPort](../../docs/contracts/trip-query-port.md); không tự sửa DTO/port theo từng nhánh.

**C1 — đã chốt bởi leader, 14/11:** bốn GET, dữ liệu/lỗi/cutoff/expiry, port liên module, bộ demo chung và thứ tự migration V2 Account → V3 Trip → V4 Booking. Baseline đã merge vào main (`f3895f6`, xác nhận bằng fetch 14/11); mọi người lấy main mới trước khi tạo branch. Không ghi approval thay thành viên. Đức Hiếu review biên bản khi nhận; leader tiếp tục hỏi TA cách chấm UC chính và nhận feedback PA3/mẫu Moodle, chưa tính các đầu vào này là đã có.

Mọi người có thể bắt đầu task theo C1. P1/H1 có thể chuẩn bị song song nhưng migration được merge/áp dụng theo dependency bên dưới. Mock/JSON examples phục vụ phát triển phải được phân biệt với API/dữ liệu demo thật. Nếu có mâu thuẫn SAD/UC hoặc vướng dependency, báo leader trước khi đổi contract.

## Phối hợp làm việc — ai bàn giao cho ai?

**Đọc trước khi bắt đầu:** [hướng dẫn phối hợp thực hành](../../docs/team-coordination.md) có việc bắt đầu của từng người, thứ tự ghép, tìm ai khi vướng và mẫu tin nhắn nhận việc/bàn giao/bị chặn. Phần dưới là bảng ownership và đầu ra chi tiết để đối chiếu.

Mỗi người thực hiện cùng một nhịp:

1. **Nhận task:** báo Hiếu ID task, nhánh, mốc dự kiến và dependency đang thiếu.
2. **Làm đúng phần mình:** trao đổi trực tiếp với người cung cấp/nhận dữ liệu; chuẩn bị phần độc lập khi đang chờ, không sửa file của họ để né dependency.
3. **Bàn giao có thể thử lại:** PR/commit + cách chạy/seed + input/expected + tests và phần còn thiếu. Người nhận thử và phản hồi, không chỉ nhắn “đã thấy”.
4. **Review và tích hợp:** tác giả sửa góp ý, reviewer rà lại, Hiếu merge theo dependency khi checks đạt; QA thực thi/retest trên build được ghi rõ.

Hiếu nhận thông tin điều phối, còn **lỗi module trả đúng tác giả**. Không đẩy mọi câu hỏi/lỗi cho leader hoặc QA; bảng tìm người xử lý ở hướng dẫn phối hợp giúp các bên làm trực tiếp.

### Một phần có một người sở hữu

Reviewer đọc, góp ý và kiểm tra; **review không có nghĩa hai người cùng code một phần**. Không sửa file/nhánh của người khác nếu chưa thống nhất. Nếu cần sửa gấp, ghi rõ ai làm file nào trước khi bắt đầu.

| Phần sở hữu | Người viết/chịu trách nhiệm | Người còn lại làm gì? |
|---|---|---|
| Account V2, tests schema Account | Trung Hiếu | Hoàng review; Huy dùng Account ID/FK đã bàn giao, không tự tạo bảng Account khác. |
| Trip V3, layout/UUID chung, API Trip, TripQueryPort | Phương | Huy review/dùng port; Hoàng dùng response API; không tự tạo lại dữ liệu Trip ở Booking/FE. |
| Booking V4, allocations, API ghế và job expiry | Huy | Phương review; Hoàng chỉ hiển thị/refresh. Trung Hiếu không viết lại logic overlap/hold thay Huy. |
| Component màn hình, điều hướng, client API FE | Hoàng | Chủ UC cung cấp dữ liệu/nhánh lỗi; Đức Hiếu kiểm tra UI. BE không sửa UI song song khi chưa báo Hoàng. |
| `SecurityConfiguration.java` và quy ước error mapping dùng chung | Trung Hiếu | Chủ API gửi method/path + response/lỗi cần map; tự viết điều kiện lỗi nghiệp vụ trong module mình. Ghép thay đổi chung sớm với leader, không tự mở `/api/**` hoặc đợi đến cuối mới báo 401. |
| Test plan/report và SAD tổng hợp | Đức Hiếu | Từng chủ module cung cấp ca, nội dung SAD và bằng chứng phần mình; tự sửa bug được giao. Trung Hiếu duyệt cuối, không viết lại toàn bộ hồ sơ. |
| OpenAPI/port/quyết định C1 | Leader quản lý quyết định; chủ phần bị ảnh hưởng đề xuất sửa | Báo field cũ/mới, lý do và bên bị ảnh hưởng; leader chốt rồi cập nhật contract trước khi hai bên đổi code. Không âm thầm đổi DTO trên nhánh riêng. |

Các thay đổi backend/FE cùng chạm build, dependency, CI hoặc security ngoài phạm vi phải báo leader theo AGENTS. Các mốc test không được bỏ để tránh conflict.

### Hợp đồng bàn giao tối thiểu

| Người gửi → người nhận | Bàn giao gì? | Lúc nào người nhận hết bị chặn? |
|---|---|---|
| Trung Hiếu → Huy, Phương | V2 đã review/merge, tên bảng/PK Account và fixture account ID tổng hợp, cách chạy test; không chỉ gửi ảnh DB cá nhân. | V2 có trên main thì Phương lấy main để merge V3 đúng thứ tự; Huy có nền Account cho FK V4. |
| Phương → Huy | Manifest Trip/Stop/seat UUID, thứ tự Stop, layout; interface/DTO/error codes TripQueryPort; link PR và ví dụ input/output. | Huy viết unit tests theo interface sớm; H2 chỉ nghiệm thu tích hợp khi port thật và V3 đã sẵn sàng. |
| Phương → Hoàng | `/stops`, `/trips`, `/trips/{tripId}`: request mẫu, JSON thật, dữ liệu demo và nhánh rỗng/lỗi; báo leader phần public route chung cần ghép. | F2/F3 nối thật sau khi endpoint và security tương ứng đã ghép; trước đó dựng UI bằng examples có ghi rõ. |
| Huy → Hoàng | API ghế thật, layout/seat IDs, serverTime/expiry và kịch bản hết ghế/hold hết hạn/lỗi tải. | F4 nối thật khi H2 sẵn sàng; không chờ có P08 tạo Booking để test P07, dùng fixture đã thống nhất. |
| Tác giả từng task → Đức Hiếu | Link PR/commit, cách seed/reset, ca chính/ca lỗi và kết quả đã chạy. | QA có thể soạn plan trước; chỉ thực thi/ghi actual khi build và dữ liệu tương ứng sẵn sàng. |

Khi bàn giao, gửi **link PR/commit + cách chạy + input/expected + điều còn thiếu**, không chỉ nhắn “xong rồi”. Người nhận thử lại và phản hồi cụ thể nếu sai contract.

### Làm song song, merge theo dependency

1. **Bắt đầu cùng lúc:** Trung Hiếu I1; Phương chuẩn bị P1/P3; Huy unit tests/logic H1; Hoàng prototype/UI theo examples; Đức Hiếu Q1. Không phải cả nhóm ngồi chờ V2 mới bắt đầu.
2. **Schema DB:** review/merge/apply V2 → V3 → V4. V2 trước V3 là thứ tự version đã thống nhất; V4 còn thật sự cần cả dữ liệu Account và Trip cho các tham chiếu. Không copy migration chưa merge của nhau rồi coi là baseline đã chia sẻ.
3. **API:** Phương ưu tiên port P3 cho Huy. P2/P4 và H2 sẵn phần nào thì báo Hiếu ghép security/error chung phần đó trong cùng PR hoặc PR phụ thuộc đã thống nhất; chạy lại tests trước merge. Không để việc mở route public chờ tới ngày demo.
4. **UI:** Hoàng ghép tìm → chi tiết → ghế theo thứ tự endpoint sẵn sàng, báo request/response lỗi cho đúng owner. Mock chỉ là bước phát triển, không là kết quả nghiệm thu.
5. **Bug và hồ sơ:** Đức Hiếu ghi bug về owner; owner sửa/test rồi QA retest. Từng người gửi phần SAD/weekly, Đức Hiếu tổng hợp, Trung Hiếu duyệt.

Khi đang chờ dependency, mở Draft PR và ghi rõ task/PR đang chờ; lấy main mới sau khi prerequisite merge rồi test lại. Không tự thêm dependency/đổi API để né chờ. Nếu hai task cùng chạm file, thống nhất một người sửa file đó và ghi vào PR trước khi code tiếp. Quy trình Git/PR chi tiết ở [handbook](../../docs/team-handbook.md).

## Minh Phương — Trip, P04/P05

Phạm vi code: `src/backend/src/{main,test}/java/vn/edu/team11/coach/trip/`, migration mới trong `src/backend/src/main/resources/db/migration/`. Không sửa V1 session. Reviewer chính: **Nhật Huy**; Trung Hiếu review migration/contract.

| ID | Task / mốc mục tiêu | Phụ thuộc | Đầu ra để nghiệm thu |
|---|---|---|---|
| P1 | Schema V3 và dữ liệu Trip — 14–15/11 | C1; merge/apply sau I1/V2 | `V3__trip_read_model.sql` theo SAD; giữ manifest UUID cho cả nhóm, Route A–B–C, sáu ghế, leg fare 100k/150k, chuyến ngày/qua đêm theo C1. Seed/reset tách schema và chỉ dùng local/test. PostgreSQL mới chạy V1→V2→V3 được. |
| P3 | Cung cấp TripQueryPort — ưu tiên 15/11 | P1; DTO/interface có thể bàn giao sớm | Facts về Trip, Stop indices, cutoff và layout theo contract; test invalid Segment, đúng cutoff, thiếu dữ liệu và lỗi port. Bàn giao interface/DTO sớm cho Huy, không đợi xong UI. |
| P2 | API danh mục + tìm chuyến — 15–16/11 | P1 | `/stops` và `/trips` đúng OpenAPI; test ngày lên tại Stop giữa tuyến, cutoff, giá cộng leg, input sai, rỗng và lỗi truy xuất. Có request/response thật để Hoàng nối. |
| P4 | API chi tiết chuyến — 16/11 | P1 | `/trips/{tripId}` có thứ tự Stop, giá đoạn và eligibility; test Trip mất/đóng bán/thiếu essential/missing optional. Không tự điền giá hoặc giờ thiếu. Cập nhật SAD phần Trip. |

P2/P3/P4 dùng chung P1 nhưng không cần chờ nhau về nghiệp vụ. Phương có thể gộp thay đổi nhỏ dùng chung vào PR đầu rồi chia PR theo endpoint/port, tránh một PR chứa toàn bộ Trip.

## Nhật Huy — Booking, P07

Phạm vi code: `src/backend/src/{main,test}/java/vn/edu/team11/coach/booking/`, migration `V4__booking_availability.sql`. Reviewer chính: **Minh Phương**; Trung Hiếu rà migration/contract.

| ID | Task / mốc mục tiêu | Phụ thuộc | Đầu ra để nghiệm thu |
|---|---|---|---|
| H1 | Schema V4 và logic ghế — 14–16/11 | C1; merge/apply sau I1/V2 + P1/V3 | Booking/BookingSeat/SeatAllocation và FK theo SAD; dùng manifest UUID của Phương, account fixture từ I1. Có fixture Confirmed, hold hiệu lực/hết hạn, cancelled/released. Test đoạn kề/giao nhau và đúng expiry. Không triển khai P08/P09. |
| H2 | API ghế trống — 16–17/11 | H1 + P3 | `/seat-availability` lấy facts qua port, map đủ ghế đúng ID; test hết ghế vẫn 200, lỗi dữ liệu không giả FREE, lỗi Segment/Trip đúng status; integration test PostgreSQL thật. |
| H3 | Cleanup hold hết hạn — 16/11 | H1 | Job Booking idempotent chuyển Pending hết hạn sang Expired, không cần người dùng mở trang; không đổi Confirmed/Cancelled. Test clock kiểm soát; gọi job hai lần không gây sai dữ liệu. Test H2 vẫn loại hold hết hạn khi cleanup trễ. Cập nhật SAD phần Booking. |

H1 unit tests có thể bắt đầu với DTO/contract đã chốt, không cần đợi P3 implementation. Chỉ dùng fixture để minh họa trạng thái Booking; không báo là đã có chức năng tạo Booking/thanh toán.

## Khánh Hoàng — giao diện xuyên suốt

Phạm vi: `src/frontend/src/features/` theo feature; API client ở `src/frontend/src/shared/api/`; thay trang foundation khi có flow thật. Reviewer chính: **Đức Hiếu**, chủ UC đối chiếu response.

| ID | Task / mốc mục tiêu | Phụ thuộc | Đầu ra để nghiệm thu |
|---|---|---|---|
| F1 | Prototype bốn màn — 14–15/11 | Đọc UC và C1 đã chốt | Home, kết quả, chi tiết, sơ đồ ghế; mỗi màn có mục đích/thông tin/cách dùng và đường đi giữa màn. Leader review flow trước khi làm kỹ giao diện. |
| F2 | Home + kết quả tìm — 15–16/11 | C1 + F1; nối thật cần P2 | Chọn Stop từ API, nhập ngày, render kết quả/giá/giờ; loading, rỗng, validation và retry có tests. Chuyển đúng tripId/fromStopId/toStopId sang chi tiết. |
| F3 | Chi tiết chuyến — 16/11 | F1; nối thật cần P4 | Hiện Stops/giá/eligibility; Trip đóng/thiếu essential không cho tiếp tục đặt. Thiếu mô tả optional vẫn xem được; giữ Segment khi đi tới ghế/quay lại. |
| F4 | Sơ đồ ghế và refresh — 16–17/11 | F1; nối thật cần H2 | Ghế theo layout, FREE/UNAVAILABLE/UNKNOWN rõ; refresh expiry/foreground/polling đúng contract. Lỗi refresh không coi ghế cũ là FREE mới; response cũ không ghi đè response mới. FE tests và kiểm tra mobile. |

Hoàng có thể dựng F2/F3/F4 bằng examples trong lúc chờ BE, nhưng chỉ nghiệm thu tích hợp sau khi nối API thật. Không thêm nút đặt/thanh toán hoạt động giả ngoài ba UC.

## Đức Hiếu — kiểm thử và hồ sơ

Phạm vi: `docs/test/`, bản SAD PA4 riêng trong `docs/analysis and design/`, kiểm tra gói ở `pa/PA4/`. Reviewer: **Hoàng** rà UI/test; **Phương/Huy** rà SAD module; **Trung Hiếu** duyệt cuối.

| ID | Task / mốc mục tiêu | Phụ thuộc | Đầu ra để nghiệm thu |
|---|---|---|---|
| Q1 | Test plan + ma trận ca — 14–15/11 | Đọc UC/C1; leader cung cấp mẫu Moodle | Ít nhất 5 ca/UC, tối thiểu 15; mỗi ca có precondition, dữ liệu, bước, expected và nguồn UC/BR. Soạn nội dung trước nếu chưa có mẫu, chuyển đúng mẫu khi nhận. Chủ UC rà expected; chưa chạy không đánh dấu pass. |
| Q2 | SAD revision PA4 — nháp 16/11, chốt 19/11 | Feedback PA3; hiện thực từng module | Tích hợp sửa mục 1–4 từ nội dung mỗi người; deployment UML/mô tả node mục 5 và cấu trúc source mục 6 khớp implementation cuối. Không ghi đè PDF PA3 gốc. |
| Q3 | Thực thi và retest — 18–19/11 | Flow tích hợp C2 + Q1 | Chạy đủ ca ít nhất một lần; ghi build/commit, môi trường, dữ liệu, actual/result và bằng chứng. Giao bug về tác giả, retest bản sửa; không thay bằng kết quả health/unit tests. |

## Trung Hiếu — điều phối và phần tích hợp chung

**Ưu tiên của bạn:** I1 trước; C2 ghép tăng dần ngay khi mỗi API sẵn sàng; C3 sau khi có đầu ra QA/hồ sơ. Bạn không phải người tự code/sửa mọi module.

| ID | Task / mốc mục tiêu | Phụ thuộc | Đầu ra để nghiệm thu / reviewer |
|---|---|---|---|
| I1 | Schema Account V2 — đã hiện thực/test 14/11, chờ review/merge | C1 | `identity_account(account_id UUID)` và constraints theo [contract I1](../../docs/contracts/identity-account.md); 35 Account IT +8 tests nền tảng pass trên PostgreSQL mới, có upgrade V1 giữ session. Fixture DISABLED tách schema, không login mặc định/P01/P02. **Hoàng review**, Đức Hiếu hỗ trợ kiểm tra. |
| C2 | Ghép từng API từ 14–17/11; chốt flow 17/11 | Từng phần theo PR endpoint sẵn sàng; flow cuối cần I1 + P2/P3/P4 + H2/H3 + F2/F3/F4 | Điều phối merge đã review; sở hữu security/error mapping chung. Ghép cùng owner endpoint ngay khi API cần route public, không chờ đủ tất cả endpoint mới bắt đầu. Chỉ mở GET đúng contract khi endpoint tồn tại; route khác vẫn bảo vệ, writes không bị mở. Chạy tests từng phần và demo xuyên suốt bằng Compose/DB thật. **Đức Hiếu review**, tác giả sửa lỗi module mình. |
| C3 | Weekly, demo và gói nộp — 20/11 | Q2/Q3 + mô tả prototype | Mỗi người gửi việc thực tế/bằng chứng; tổng hợp weekly từ 07/11, chạy thử demo, kiểm tra gói PA4-Group11. Đức Hiếu đóng gói hỗ trợ, Trung Hiếu duyệt/nộp ngày 21/11 và lưu bằng chứng. Không tự ghi đã nộp khi chưa có kết quả. |

### Checklist riêng cho Trung Hiếu

- [ ] **Phát tài liệu và nhận việc:** chia sẻ C1/TASKS/handbook qua PR, nhận phản hồi owner và mốc thực tế; tiếp tục lấy mẫu Moodle/feedback và hỏi TA về UC chính. Khi tài liệu chưa merge, nhóm đọc đúng nhánh/PR được chia sẻ, không mặc định main đã có bản mới.
- [ ] **I1 — thiết kế schema Account:** đối chiếu SAD: accountId, email unique, passwordHash, fullName, phone, status; một role thuộc PASSENGER/STAFF/ADMINISTRATOR. Chốt tên bảng/PK, nullability/constraints với Hoàng reviewer để Huy dùng FK; không tự mở thêm login/register API hoặc tạo AuthenticationService trong task này.
- [ ] **I1 — viết V2 + tests:** tạo `src/backend/src/main/resources/db/migration/V2__identity_account.sql` và integration test Account dưới `src/backend/src/test/java/vn/edu/team11/coach/identity/` (test JDBC được, không bắt buộc dựng controller/repository không dùng). Kiểm tra DB mới V1→V2, UUID/PK, email unique và role constraints đã thiết kế; V1/session/health vẫn chạy. Test không dùng DB thật.
- [ ] **I1 — dữ liệu và bàn giao:** fixture Account tổng hợp tách SQL schema, không credential mặc định có thể login. Gửi Huy tên PK/account ID dùng chung và cách nạp; chạy backend verify thật, mở PR I1, nhờ Hoàng review. Sau merge báo Phương lấy main mới trước V3.
- [ ] **C2 — review/ghép theo PR:** nhận method/path + lỗi từ Phương/Huy, cập nhật phần chung và tests trong luồng PR đã thống nhất; không lấy việc điều phối làm lý do tự approve PR của mình. Bug nghiệp vụ trả đúng owner; Đức Hiếu rà tích hợp.
- [ ] **C3 — chốt sản phẩm:** dựa trên report QA/đầu ra từng người để tổng hợp weekly, duyệt demo/gói nộp; không tự viết thay toàn bộ test plan/SAD/frontend/backend. Chỉ ghi completed khi có bằng chứng.

## Cách nhận và nộp task

- Một task có một owner; người khác hỗ trợ phải thống nhất file để tránh đè nhau. Đặc biệt migration và security/error mapping phải phân người sửa trước.
- Task sẵn sàng khi input/contract cần thiết đã chốt; mỗi người nhận task đầu tiên trước, không mở nhiều nhánh phụ thuộc cùng lúc khi chưa cần.
- PR ghi ID task, UC/BR, cách chạy, dữ liệu demo, nhánh lỗi và kết quả kiểm tra. Backend dùng verify + PostgreSQL test riêng; FE lint/typecheck/test/build theo [AGENTS](../../AGENTS.md). Chưa chạy ghi rõ chưa chạy.
- Có review của người khác, kiểm tra đạt và đáp ứng đầu ra mới đánh dấu xong. Agent review không thay approval của thành viên. Task code phải kèm tests/tài liệu phần mình; QA không nhận trách nhiệm sửa mọi module.

**Nhận việc ngay:** Trung Hiếu theo dõi review I1 đã có code/test và chuẩn bị C2 theo từng endpoint; Phương làm P1 và ưu tiên bàn giao P3; Huy bắt đầu logic/tests H1, chờ V2/V3 trên main để ghép schema; Hoàng review I1 và làm F1/F2; Đức Hiếu làm Q1. Kiểm tra trạng thái PR I1 hiện tại trước khi báo còn chờ hay đã merge; không giao lại phần đã làm. Mỗi người gửi ID task nhận, nhánh dự kiến và vướng mắc. Không cần chờ QA/SAD hoàn tất mới làm code.
