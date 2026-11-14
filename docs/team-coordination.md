# Team 11 — phối hợp để làm xong PA4

Đọc cùng [TASKS](../pa/PA4/TASKS.md). File này trả lời **mình làm trước việc gì, cần ai và gửi gì cho người tiếp theo**; lệnh Git/push/PR nằm trong [handbook](team-handbook.md). C1 đã được leader chốt, nhưng từng người vẫn phải xác nhận nhận việc/lịch thực tế. Không coi tài liệu này là bằng chứng task đã hoàn thành.

## 1. Mỗi người bắt đầu thế nào?

| Người | Bắt đầu ngay | Cần phối hợp với ai? | Khi có đầu ra phải báo |
|---|---|---|---|
| **Trung Hiếu** | Theo dõi review I1, điều phối V2→V3→V4; nhận yêu cầu ghép security/lỗi chung từng API. I1 đã có code/test, không giao làm lại. | Chủ API gửi path/lỗi cần ghép; Đức Hiếu review tích hợp. | Sau khi I1 merge, báo Phương/Huy cập nhật main; sau mỗi API ghép xong báo Hoàng/QA cách thử. |
| **Minh Phương** | P1 schema Trip/dữ liệu chung, ưu tiên P3 port cho Huy; sau đó P2/P4 API tìm/chi tiết. | Nhận PK Account từ Hiếu; thống nhất interface TripQueryPort và dữ liệu mẫu với Huy sớm. | Gửi Huy port + UUID/layout; gửi Hoàng API/request/JSON thật; gửi Hiếu phần route chung cần mở. |
| **Nhật Huy** | H1 logic/tests Segment và allocations; chuẩn bị V4 theo contract, chưa chờ đủ API/UI mới viết test. | Dùng Account từ Hiếu, Trip/layout/Stop order từ Phương; không tự dựng lại các phần đó. | Gửi Hoàng API ghế/kịch bản expiry; gửi Hiếu phần route/lỗi chung cần ghép; gửi Phương PR để review. |
| **Khánh Hoàng** | Làm F1 prototype và UI F2–F4 theo examples; nối thật từng API khi được bàn giao. | Phương cho API Trip; Huy cho API ghế; Hiếu xử lý phối hợp phần security chung. | Gửi Đức Hiếu flow/cách thử; báo đúng chủ API khi có lỗi, kèm request/response thực tế. |
| **Đức Hiếu** | Q1 test plan/cases ngay từ UC/contract; nhận nội dung SAD từng người để tổng hợp Q2. | Chủ UC rà expected; Hiếu cung cấp mẫu Moodle/feedback khi có. | Giao bug cho tác giả module; báo kết quả test/retest và hồ sơ còn thiếu cho Hiếu. |

**Owner code, reviewer kiểm tra.** Phương/Huy không cùng viết một migration; reviewer không tự sửa nhánh tác giả nếu chưa thống nhất. QA ghi lỗi và retest, không nhận sửa mọi module. Leader điều phối, không code lại toàn bộ sản phẩm.

## 2. Thứ tự ghép để không phải chờ nhau toàn bộ

1. **Chuẩn bị song song:** Phương viết Trip; Huy viết logic/tests ghế; Hoàng dựng UI; Đức Hiếu viết cases. Dùng cùng contract, dữ liệu minh họa phải ghi rõ là mock/example.
2. **Đưa schema lên main:** I1/V2 của Hiếu → P1/V3 của Phương → H1/V4 của Huy. Trước khi dùng, kiểm tra PR đã merge và lấy main mới; không chỉ nhìn thấy file trên một nhánh rồi báo prerequisite đã xong.
3. **Ghép API theo phần sẵn sàng:** Phương bàn giao P3 sớm để Huy nối H2; P2/P4 sẵn thì Hoàng nối F2/F3; H2 sẵn thì nối F4. Hiếu ghép security/error chung cùng từng PR, không chờ tới ngày demo.
4. **Chạy xuyên suốt:** tìm chuyến → chi tiết → ghế trên cùng Segment. Đức Hiếu chạy cases trên commit và bộ dữ liệu được ghi rõ; bug quay về đúng owner, sửa xong mới retest.

Task đang chờ vẫn có thể mở Draft PR ghi dependency. Chỉ tính **sẵn sàng tích hợp** khi code, security cần thiết, dữ liệu/cách chạy và checks liên quan đều đủ. “Chạy trên máy mình” hoặc JSON hardcode chưa đủ để bên nhận ghép thật.

## 3. Khi vướng, tìm đúng người

| Vướng mắc | Người xử lý đầu tiên | Thông tin cần gửi |
|---|---|---|
| Chưa biết UUID/Stop order/layout, ghép port không được | Phương; Huy phối hợp phần consumer | Input, kiểu DTO/method đang dùng, kết quả mong đợi và lỗi thực tế. |
| API chưa có hoặc trả sai dữ liệu | Phương với Trip; Huy với ghế | Method/URL/query, response/status và contract đang đối chiếu. |
| FE gửi sai tham số hoặc hiển thị sai dù response đúng | Hoàng | Steps, request/response và ảnh/trạng thái UI. |
| Public GET bị 401/403 hoặc cần error format chung | Hiếu + chủ API | Path/method, response thực tế, PR endpoint; không tự permit cả `/api/**` hoặc tắt CSRF. |
| Migration trùng version, lỗi FK hoặc thiếu prerequisite | Owner migration + Hiếu; bên cung cấp schema liên quan tham gia | Lỗi SQL/Flyway, version/schema/PR đang dùng; không sửa migration người khác hay xóa DB dùng chung để thử. |
| Hai người cần sửa cùng file hoặc contract không đủ | Báo Hiếu và người đang sở hữu phần đó | File/field cũ–mới, lý do, người bị ảnh hưởng; chốt một người sửa trước khi tiếp tục. |

Báo ngay trong buổi làm việc khi thấy dependency đang chặn mình, không im lặng chờ tới hạn. Nêu rõ còn phần nào có thể làm tiếp. Nếu chưa thống nhất hoặc có nguy cơ trễ mốc, Hiếu quyết định ưu tiên/phạm vi; không tự đổi nghiệp vụ.

## 4. Gửi và nhận một phần việc

**Tác giả gửi:** link PR + commit, phần đã làm/chưa làm, cách seed/chạy, request hoặc thao tác mẫu, expected chính và lỗi, tests đã chạy. Không gửi secret hoặc dump dữ liệu thật.

**Người nhận làm:** xác nhận đã đọc, thử bằng đúng commit/dữ liệu, phản hồi đã nối được hoặc chỉ ra lỗi. Đọc trước để chuẩn bị không đồng nghĩa được dùng nhánh chưa merge làm baseline chung. Nếu cần nhánh phụ thuộc đặc biệt, thống nhất với Hiếu trước.

**Khi thay đổi ảnh hưởng người nhận:** báo trước khi sửa field/enum/path/port/UUID dùng chung; cập nhật contract rồi cả hai bên điều chỉnh. Không gửi bản mới bất ngờ sau khi bên kia đã code theo bản cũ. Giữ một manifest dữ liệu Trip/Stop/ghế do Phương quản lý; Huy bổ sung allocations tham chiếu các ID đó.

Mẫu tin nhắn đủ dùng:

```text
NHẬN VIỆC: Mình nhận P1, nhánh feat/p1-trip-schema; dự kiến ...; đang cần ...

BÀN GIAO: P3 / PR ... / commit ... đã có interface + implementation ...
Gửi Huy: input mẫu ..., expected ..., chạy bằng ...; tests ...; còn thiếu ...

BỊ CHẶN: H2 đang chờ ... từ P3; lỗi/request thực tế ...; cần ...
Trong lúc chờ mình làm tiếp ...; nếu không có trước ... thì ảnh hưởng mốc ...
```

## 5. Review, bug và tiến độ

- Tác giả tự kiểm tra trước khi nhờ review; gửi reviewer đúng TASKS. Reviewer phản hồi điểm cần sửa hoặc approve sau khi đã rà, không approve vì nể/để kịp hạn.
- Bug gồm steps + dữ liệu/commit + expected/actual. Owner sửa trên PR tương ứng hoặc PR fix được thống nhất, chạy test; Đức Hiếu retest rồi mới ghi kết quả mới.
- Cuối buổi báo task, PR/commit, kiểm tra đã chạy, đang chờ ai và bước tiếp theo. Những mục không có bằng chứng phải để “chưa chạy/chưa xong”.
- Hiếu merge sau review/checks đạt; mỗi người cập nhật main trước task kế tiếp. Không tự merge hộ hoặc force push lên nhánh của người khác.

## 6. Deadline nội bộ đã chốt

Leader chốt lịch dưới đây ngày 14/11/2026. “Tôi” là Liên Trung Hiếu. Các ngày đều thuộc tháng 11/2026, giờ Việt Nam. Không đưa mốc 15/11 vào lịch này; việc chốt trước đó không giao lại.

| Hạn bàn giao | Người phụ trách | Đầu ra cần có |
|---|---|---|
| **16/11** | Phương | P1 schema V3 + dữ liệu mẫu/manifest ID và P3 TripQueryPort cho Huy ghép; review/merge schema theo dependency. |
| **16/11** | Huy | Logic ghế theo đoạn + unit tests H1; chuẩn bị V4 để ghép sau V2/V3. |
| **16/11** | Hoàng | F1 prototype bốn màn và khung UI để nối API từng phần. |
| **16/11** | Đức Hiếu | Bản đầu Q1: ít nhất 5 test cases/UC, tối thiểu 15 ca; chủ UC rà expected. |
| **18/11** | Phương + Huy | P2/P4 và H2/H3: API tìm chuyến, chi tiết, ghế và cleanup expiry chạy được; có tests, dữ liệu và cách thử để FE/QA nhận. |
| **19/11** | Hoàng + tôi | F2–F4/C2: nối FE–BE, security/error chung; chạy được luồng tìm → chi tiết → ghế trên cùng Segment bằng dữ liệu thật trong môi trường thử. |
| **20/11 trước 12h** | Đức Hiếu + cả nhóm | Q2/Q3: chạy đủ cases, ghi actual/bằng chứng và bug; hoàn thiện SAD, phần weekly report và bằng chứng của từng người để tôi tổng hợp. |
| **20/11 trước 20h** | Cả nhóm; tôi chốt | Sửa bug, retest, chạy demo; hoàn tất C3 và chốt gói nộp. Lỗi chưa giải quyết phải ghi rõ, không bỏ kiểm tra để kịp mốc. |
| **21/11** | Tôi | Kiểm tra gói bài và nộp trước giờ chính thức trên Moodle, lưu bằng chứng nộp. Giờ nộp cụ thể cần đối chiếu Moodle. |

**Hết 19/11 phải chạy được luồng xuyên suốt.** Ngày 20 dành cho kiểm thử, sửa lỗi và hồ sơ. Đây là hạn bàn giao, không phải đợi đến hạn mới mở PR: gửi từng phần nhỏ để review và tích hợp sớm. Nếu có nguy cơ trễ, báo tôi ngay cùng phần còn thiếu, người đang chờ và phương án điều chỉnh; không tự lùi deadline.
