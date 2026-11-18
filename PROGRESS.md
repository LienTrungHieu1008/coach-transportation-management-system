# PA4 — tiến độ và phân công tiếp theo, 17/11/2026

“Tôi” là Liên Trung Hiếu. Phạm vi: P04 tìm chuyến, P05 chi tiết chuyến, P07 xem ghế. Trạng thái theo lần đối chiếu ngày 17/11; **“chưa ghi nhận tiến độ thực hiện”** nghĩa là chưa có bằng chứng được gửi/ghi nhận, không kết luận thành viên chưa làm.

## 1. Tổng hợp theo thành viên

| Thành viên | Đã làm | Chưa hoàn thành / cần làm tiếp |
|---|---|---|
| **Tôi** | Chốt C1/API; baseline, Account V2 và tài liệu bàn giao đã merge. Đã review/sửa F1 của Hoàng và P3 của Phương. | Chưa review Huy (#5). Cần chốt tích hợp H1, ghép API/security, kiểm tra flow thật và tổng hợp gói nộp. |
| **Phương** | Schema Trip V3, dữ liệu mẫu và interface P3 đã merge. Adapter P3 ở #6 được tôi hỗ trợ sửa/test, CI đạt. Đã review core H1 của Huy. | Đọc lại hai commit sửa P3; phối hợp Huy review để merge. **P2 API danh mục/tìm chuyến và P4 chi tiết: chưa ghi nhận tiến độ thực hiện.** |
| **Huy** | Core logic ghế và 63 unit tests ở #5; Phương đã nhận xét đồng ý core. | Tôi chưa review. **V4, lưu/đọc Booking từ DB, H2 API ghế và H3 cleanup: chưa ghi nhận tiến độ thực hiện.** Cần review lại P3 và chốt đường đưa #5 vào main. |
| **Hoàng** | F1 bốn màn ở #7; tôi đã sửa tests/ngày demo, cải thiện thiết kế và kiểm tra trực quan. CI đạt. | Đọc lại phần sửa; hoàn thiện mô tả bốn màn, nhờ Đức Hiếu review. **F2–F4 nối API thật: chưa ghi nhận tiến độ thực hiện.** |
| **Đức Hiếu** | **Chưa ghi nhận tiến độ thực hiện** Q1 test plan/cases, Q2 SAD PA4 và Q3 báo cáo chạy/retest. | Gửi path/nhánh nếu đã làm. Ưu tiên Q1 tối thiểu 15 ca; review F1, tổng hợp SAD và chạy/retest khi có build thật. |

PR #5/#6/#7 chưa merge tại lần đối chiếu. CI xanh hoặc có unit tests chưa đồng nghĩa hoàn thành UC hay đã thực thi đủ 15 ca PA4.

## 2. Phần tôi đã review/sửa — thành viên đọc lại

### Hoàng — PR #7, nhánh `feat/f1-prototype`

- `68ba5f0`: sửa tests cũ gây CI fail; thống nhất thời điểm demo và ghi nhãn dữ liệu mẫu.
- `2f7b504`: cải thiện bố cục, màu, chữ, mobile/dark/focus; giữ yêu cầu và luồng F1.
- **Hoàng làm tiếp:** đọc lại thay đổi, rà mô tả mục đích/thông tin/cách dùng bốn màn; gửi Đức Hiếu review. F1 hiện dùng dữ liệu mẫu, chưa nối backend.

### Phương — PR #6, nhánh `feat/p3-trip-postgres-adapter`

- `3504835`: kiểm tra đủ giá **toàn chuyến**; thiếu B–C thì truy vấn A–B cũng trả `ESSENTIAL_DATA_INCOMPLETE`. Đã thêm test lỗi và ca đủ giá.
- `122d965`: thêm test PostgreSQL chứng minh transaction cũ giữ snapshot, transaction mới thấy cập nhật; dùng latch, không sleep. Đây là bổ sung bằng chứng, không kết luận transaction ban đầu sai.
- **Phương làm tiếp:** đọc hai commit và `docs/test/p3-review-fixes-handoff.md` trên nhánh P3; phản hồi trên #6 rằng đã hiểu và tiếp nhận phần sửa. Huy review lại đúng head `122d965`. Chưa kiểm chứng snapshot xuyên Trip–Booking.

Xem thay đổi mà không chuyển/reset nhánh đang làm:

```bash
git fetch origin
git show 3504835
git show 122d965
```

Bằng chứng đã có: P3 head `122d965` đạt CI với 90 backend tests; F1 head `2f7b504` đạt CI, FE có 15 tests. Lượt verify cuối của P3 ở local chưa hoàn tất do Docker không phản hồi; kết quả xanh cuối thuộc GitHub CI. Huy ghi nhận 130 tests trên nền P1 trong báo cáo của mình; **tôi chưa chạy lại/review Huy**. Lần sửa tài liệu này không chạy lại ứng dụng.

## 3. Timeline — ai phải bàn giao gì?

Các mốc theo giờ Việt Nam, tháng 11/2026. **Bàn giao từng phần ngay khi xong, không chờ tới hạn mới tạo PR.** Thành viên xác nhận nhận việc; phần đã làm ngoài repo thì gửi path/nhánh để cập nhật, tránh giao trùng.

| Mốc | Tôi | Phương | Huy | Hoàng | Đức Hiếu |
|---|---|---|---|---|---|
| **17/11** | Review #5; cùng Huy chốt đường tích hợp H1. Theo dõi review #6/#7. | Đọc hai commit sửa P3, phản hồi #6; tiếp tục P2/P4. | Review lại #6; phối hợp xử lý base #5; làm V4/H2/H3. | Đọc sửa F1, hoàn thiện mô tả bốn màn; chuẩn bị client/state F2–F4. | Gửi Q1 nháp tối thiểu 15 ca, 5 ca/UC; review F1 #7. |
| **18/11 — hết ngày** | Ghép security/error với từng API được bàn giao. | Bàn giao P2/P4: API thật, request/response, dữ liệu mẫu và tests. | Bàn giao V4/persistence, H2 API ghế, H3 cleanup; tests PostgreSQL và fixture. | Nối từng API đã sẵn sàng, báo lỗi đúng owner ngay. | Rà expected với chủ UC, chạy ca theo build sẵn có; nhận SAD từng người. |
| **19/11 — hết ngày** | Cùng owner xác nhận flow thật trên DB; xử lý điểm ghép chung. | Sửa lỗi Trip khi FE/QA ghép; gửi SAD/weekly phần mình. | Sửa lỗi ghế/expiry khi ghép; gửi SAD/weekly phần mình. | Chốt F2–F4: tìm → chi tiết → ghế, giữ Segment, refresh/lỗi; FE checks + mobile. | Chạy đủ ca trên flow tích hợp, ghi bug/bằng chứng; tổng hợp SAD. |
| **20/11 — trước 12h** | Nhận đủ đầu ra, tổng hợp weekly và rà hồ sơ. | Gửi đủ SAD/weekly/bằng chứng; xử lý bug Trip. | Gửi đủ SAD/weekly/bằng chứng; xử lý bug Booking. | Gửi mô tả prototype/weekly/bằng chứng; xử lý bug UI. | Chốt SAD và report tối thiểu 15 ca đã thực thi, actual/result/bug rõ. |
| **20/11 — trước 20h** | Duyệt demo và gói nộp C3. | Sửa lỗi/retest cùng QA, hỗ trợ demo. | Sửa lỗi/retest cùng QA, hỗ trợ demo. | Sửa lỗi/retest cùng QA, hỗ trợ demo. | Retest bản sửa; kiểm tra hồ sơ/gói bài. |
| **21/11** | Xác minh giờ Moodle, nộp và lưu bằng chứng. | Hỗ trợ nếu có lỗi đóng gói/demo. | Hỗ trợ nếu có lỗi đóng gói/demo. | Hỗ trợ nếu có lỗi đóng gói/demo. | Hỗ trợ kiểm tra gói cuối. |

## 4. Điểm phải chờ và cách làm song song

| Việc bị chặn | Nhắn ai / cần chốt gì? | Trong lúc chờ vẫn làm được |
|---|---|---|
| Đưa core H1 (#5) vào main; base hiện là nhánh P1 | **Huy + tôi:** chốt đích tích hợp và chạy checks trên nền nhận cuối. | Huy chuẩn bị V4/persistence, fixture và H3. |
| H2 ghép Trip trong cùng snapshot | **Huy + Phương:** review lại #6, thống nhất caller transaction và `asOf`; báo tôi để điều phối merge. | Huy làm logic/tests/H3 và chuẩn bị H2 theo port. **Không cần đợi P2/P4 xong.** |
| F2/F3 chưa có API thật | **Hoàng nhắn Phương:** cần P2/P4, request/response và dữ liệu thử. | API client, loading/rỗng/lỗi, điều hướng giữ Segment theo contract. |
| F4 chưa có API ghế thật | **Hoàng nhắn Huy:** cần H2, dữ liệu ghế/expiry và cách thử. | Layout, refresh/state/tests theo contract; ghi rõ dữ liệu mẫu. |
| API bị chặn login hoặc lỗi chung không khớp | **Owner API nhắn tôi:** path/method, request, actual/expected cần sửa. | Tiếp tục phần nghiệp vụ/test độc lập. |
| Thiếu đầu vào SAD/test/weekly | **Đức Hiếu nhắn đúng owner**; mẫu Moodle/feedback/TA thì nhắn **tôi**. | Soạn Q1/SAD nháp; chỉ ghi kết quả test sau khi thực thi thật. |

Tham khảo trong repo khi cần: `pa/PA4/TASKS.md` (tiêu chí task), `pa/PA4/C1_DECISIONS.md` (quyết định chung), `docs/contracts/openapi.yaml` và `docs/contracts/trip-query-port.md` (API/port), `docs/test/p1-trip-demo-manifest.md` (ID dữ liệu chung), `docs/team-handbook.md` (Git/PR).

## 5. Quy tắc báo tiến độ, tạo PR và xử lý chặn

1. **Làm xong phần nào:** chạy kiểm tra liên quan, push nhánh và tạo PR ngay; báo **tôi + reviewer + người nhận đầu ra**, không đợi cuối ngày/deadline. Phần chưa xong có thể mở Draft PR nhưng phải ghi rõ còn thiếu. Có PR sẵn thì cập nhật chính PR đó và báo commit mới.
2. **PR cần đủ:** task, đã làm/còn thiếu, cách chạy và dữ liệu thử, kết quả kiểm tra, ít nhất một nhánh lỗi. Chưa chạy kiểm tra nào thì ghi rõ; không bỏ test để kịp mốc. Tạo PR không có nghĩa đã được merge.
3. **Gặp chặn:** nhắn trực tiếp **người đang cung cấp đầu vào/người cần quyết định** ngay. Nêu đang cần họ **chốt gì, sửa gì hoặc bàn giao gì**, cần trước khi nào, ảnh hưởng task/mốc nào; báo tôi để theo dõi. Tra người phụ trách ở mục 5.
4. **Người nhận yêu cầu:** phản hồi việc sẽ xử lý và thời điểm bàn giao. Khi đã xử lý, báo lại người đang chờ kèm PR/commit/path để họ tiếp tục ngay. Nếu không giải quyết kịp, hai bên báo tôi để chốt ưu tiên, không im lặng chờ tới deadline.
5. **Cần đổi API/DTO/port/ID/migration:** nhắn owner và bên bị ảnh hưởng để chốt trước khi sửa; tôi điều phối quyết định chung. Không tự đổi contract hoặc sửa migration đã chia sẻ để né chặn.
6. **Review và hoàn tất:** Huy review Trip, Phương review Booking, Đức Hiếu review UI/tích hợp; tôi rà phần chung và điều phối merge. Tác giả sửa góp ý, báo lại commit mới; đủ review/checks mới merge. Cuối buổi mỗi người vẫn gửi cập nhật ngắn, kể cả chưa xong.

Mẫu báo dùng ngay:

```text
XONG: [task] — PR #... / commit ...
Đã làm: ... | Kiểm tra: ... | Còn thiếu: ...
Nhờ [reviewer] review; [người nhận] có thể bắt đầu ...

BỊ CHẶN: [task] đang cần [tên người] chốt/sửa/bàn giao ...
Cần trước: ... | Ảnh hưởng nếu trễ: ...
Trong lúc chờ tôi làm: ... | Nhờ phản hồi thời điểm xử lý.

CHỐT TASK: [task] — đã làm ... — PR/commit/path ...
Đang chờ ai/việc gì: ... | Bước tiếp theo + hạn: ...
```
