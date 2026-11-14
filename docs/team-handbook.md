# Team 11 — handbook làm việc trong repo

Cập nhật 14/11/2026. Đọc phần 1–3 để bắt đầu; dùng các phần còn lại khi push/PR/review. Quy tắc gốc ở [AGENTS](../AGENTS.md), [CONTRIBUTING](../CONTRIBUTING.md); contract nghiệp vụ không bị thay thế bởi handbook này.

## 1. Tôi cần làm gì trước tiên?

1. Mở [TASKS](../pa/PA4/TASKS.md), tìm tên mình; đọc đầu ra, phụ thuộc và reviewer. Phản hồi leader task nhận, mốc dự kiến, điểm đang vướng.
2. Đọc [C1 đã chốt](../pa/PA4/C1_DECISIONS.md), [OpenAPI](contracts/openapi.yaml), [port Trip](contracts/trip-query-port.md) và UC/BR của task trong [tài liệu nguồn](references.md). Chỉ cần đọc sâu phần liên quan, không đoán yêu cầu từ tên task.
3. Lấy main mới, tạo nhánh riêng cho task. Chạy baseline trước để phân biệt lỗi môi trường với lỗi mình tạo.
4. Làm từng phần nhỏ có thể demo và kiểm tra; gửi PR cho đúng reviewer, sửa góp ý trên cùng nhánh. Trung Hiếu quản lý merge.

**Phạm vi PA4:** tìm chuyến → chi tiết → ghế trống. Chưa làm login/đặt vé/thanh toán/AI. Bốn GET public đã có contract, chưa có implementation nghiệp vụ trong baseline. I1 chỉ chuẩn bị Account cho FK, không phải giao Hoàng hoặc Trung Hiếu làm toàn bộ authentication.

| Người | Việc bắt đầu | Reviewer chính |
|---|---|---|
| Trung Hiếu | I1: Account V2 ưu tiên; C2 ghép từng API ngay khi sẵn sàng | Hoàng review I1; Đức Hiếu review C2 |
| Minh Phương | P1: Trip V3; ưu tiên P3 port cho Huy | Nhật Huy |
| Nhật Huy | H1: logic/tests allocations, schema V4 sau V2/V3 | Minh Phương |
| Khánh Hoàng | F1 prototype rồi F2 tìm chuyến; review I1 | Đức Hiếu và chủ UC |
| Đức Hiếu | Q1 test plan/cases, nhận nội dung SAD từ chủ module | Hoàng/chủ module; leader duyệt cuối |

Task chưa nhận hoặc chưa làm xong phải ghi đúng trạng thái; đừng dùng tỷ lệ hoàn thành tài liệu PA3 để báo code PA4 đã chạy.

Mục **Phối hợp làm việc** trong TASKS quy định owner file, dữ liệu bàn giao và lúc nào cần chờ nhau. Xem thêm [hướng dẫn phối hợp thực hành](team-coordination.md) để biết mình bắt đầu từ đâu, liên hệ ai khi vướng và dùng mẫu tin nhắn bàn giao. Review không có nghĩa cùng viết một file; khi có bug trả về tác giả module. `V1/V2/V3/V4` là version migration SQL của database, khác mã task I1/P1/H1 và mã UC; TASKS giải thích cụ thể từng file. C2 ghép security/error chung theo từng PR endpoint, không đợi tới ngày tích hợp cuối.

## 2. Hiểu nhanh Git và PR

| Khái niệm | Hiểu để thao tác |
|---|---|
| `main` | Bản dùng chung đã được review/merge. Không push trực tiếp. |
| Branch | Nhánh làm task riêng; ví dụ `feat/p2-search-trips`. |
| Commit | Ghi một phần thay đổi vào lịch sử local. Chưa tự gửi lên GitHub. |
| Push | Gửi commit lên nhánh trên GitHub. Không có nghĩa đã nhập vào main. |
| Pull request (PR) | Yêu cầu review và nhập nhánh task vào main. |
| Review | Thành viên khác kiểm tra code/tài liệu/test và phản hồi. |
| CI | GitHub chạy kiểm tra tự động. Xanh là điều kiện tối thiểu, không thay review. |
| Merge | Đưa thay đổi được duyệt vào main. Do Trung Hiếu thực hiện theo phân công. |

Luồng chung: **nhận task → branch → code/test → commit/push → PR → review/sửa → CI xanh → leader merge → cập nhật main**.

## 3. Lấy repo và tạo nhánh

Nếu chưa có repo trên máy:

```powershell
git clone https://github.com/LienTrungHieu1008/coach-transportation-management-system.git
Set-Location coach-transportation-management-system
```

Nếu đã clone, mở terminal đúng thư mục repo, không clone chồng vào repo cũ. Trước mỗi task:

```powershell
git status
git switch main
git pull --ff-only origin main
git switch -c feat/p2-search-trips
```

Tên nhánh trên là **ví dụ task P2**; đổi theo phần mình: `feat/i1-account-schema`, `feat/p1-trip-schema`, `feat/p3-trip-port`, `feat/h1-seat-allocation`, `feat/f1-prototype`, `docs/q1-test-plan`. Dùng `fix/<task>-<mo-ta>` cho lỗi, `docs/<task>-<mo-ta>` cho tài liệu. Một nhánh cho một task/PR; không dùng lại nhánh PR đã merge.

Chỉ chạy khối tạo nhánh khi thay đổi local đã được xử lý rõ ràng. Nếu `git status` còn thay đổi, xem từng file trước; đừng đổi nhánh và mang nhầm công việc task khác theo. Nếu `pull --ff-only` báo không thể fast-forward, dừng và nhờ leader xem lịch sử; không dùng reset để ép giống remote.

Kiểm tra đang ở nhánh nào bằng `git branch --show-current`. Nếu Git báo thiếu tên/email khi commit, cấu hình **trong repo** với tên/email GitHub của chính mình; không chép danh tính của thành viên khác. Không chia sẻ token/password để nhờ người khác push hộ.

## 4. Chạy local và kiểm tra trước PR

Hướng dẫn đầy đủ: [development](development.md). Từ gốc repo, Docker Desktop chạy Linux containers:

```powershell
docker compose up --build -d --wait
docker compose ps
```

Mở `http://localhost:8088`; health tại `/api/health`. Health UP chỉ chứng minh kết nối nền tảng, không chứng minh UC hoàn thành. Sau khi sửa code phải build lại hoặc dùng chế độ dev theo development.md; không kiểm tra nhầm image cũ.

FE dùng Node theo `.nvmrc`. Từ gốc repo, chạy từng lệnh, dừng sửa lỗi nếu exit code khác 0:

```powershell
Set-Location src/frontend
npm.cmd ci
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
Set-Location ../..
```

BE dùng Java 21 và PostgreSQL 16 test riêng. Cách không cần cài JDK host, từ gốc repo:

```powershell
docker compose -f compose.test.yaml run --rm backend-test
docker compose -f compose.test.yaml down
```

Chạy lệnh down sau khi test kết thúc để dọn test services; không thêm `-v` hoặc dùng dữ liệu thật. Nếu đã có JDK/DB test, cấu hình datasource test rồi chạy Maven Wrapper theo development.md. Maven verify gồm unit + integration tests; package/build image không thay verify.

Chỉ sửa tài liệu thì kiểm tra nội dung, link, format và schema/example nếu liên quan; không cần tự chạy lại toàn bộ app tests không liên quan. Ghi đúng các lệnh đã chạy và phần chưa chạy. Không tắt lint/test/typecheck, thêm skip hoặc dùng dữ liệu giả để CI xanh.

## 5. Commit và push đúng phần việc

Xem thay đổi trước:

```powershell
git status --short
git diff
```

Trong VS Code Source Control, dùng dấu **+ ở từng file thuộc task** để stage, rồi kiểm tra bằng terminal. Có thể dùng `git add -- path/to/file` với đường dẫn thật thay `path/to/file`; đừng copy nguyên placeholder. Không mặc định stage tất cả khi repo có thay đổi của người khác/task khác.

```powershell
git diff --cached --stat
git diff --cached
git diff --cached --check
git commit -m "feat(P2): implement trip search"
git push -u origin feat/p2-search-trips
```

Đổi message và tên nhánh cho đúng task. Các lần sửa tiếp trên nhánh đã có upstream chỉ cần commit rồi `git push`.

- Không commit `.env` thật, token, secret, dữ liệu khách hàng, node_modules, dist, target hoặc log. `.env.example` chỉ chứa giá trị mẫu công khai. Gitignore không phải công cụ bảo đảm mọi secret đã bị chặn.
- Không push trực tiếp main, force push, sửa lịch sử nhánh dùng chung hoặc dùng `git reset --hard`/`git clean -fd` để chữa lỗi không hiểu. Nếu lỡ commit secret, báo leader ngay; chỉ xóa file khỏi commit sau không thu hồi được secret đã lộ.
- Có thể push phần dở dang để mở **Draft PR**, ghi rõ chưa xong/check chưa chạy. Draft không được merge; không sửa CI để che lỗi của phần chưa xong.
- Đẩy code lên nhánh **chưa có PR** không tự chạy CI của repo hiện tại: workflow chạy PR vào main và push main. Mở PR để có checks; không thấy CI sau push nhánh riêng chưa có nghĩa cấu hình hỏng.

## 6. Mở PR và sửa sau review

Trên GitHub, mở **Pull requests → New pull request**, chọn **base: main**, compare là nhánh task. Nếu còn thiếu phần đã hẹn, mở Draft; khi đủ đầu ra và kiểm tra cần thiết, chuyển **Ready for review**.

Tiêu đề có ID: `feat(P2): tìm chuyến theo Stop và ngày lên xe`. Điền [PR template](../.github/pull_request_template.md), tối thiểu:

```text
Task/UC: P2 / UC-P04, BR-P04-01..05
Thay đổi: endpoint, service/module và hành vi được bổ sung
Ảnh hưởng: API/port/migration/dependency/security; không có thì ghi không có
Cách thử: dữ liệu, request/thao tác, expected; có một nhánh lỗi
Kiểm tra: lệnh đã chạy + kết quả, phần chưa kiểm chứng
Cần reviewer chú ý: ngày qua đêm, cutoff, giá hoặc điểm khó của task
```

Chọn reviewer theo TASKS và gửi link PR cho reviewer/leader qua kênh nhóm. Một người khác review, tác giả không tự approve. Với migration/contract/security, thêm Trung Hiếu vào vòng rà; PR Trung Hiếu viết vẫn cần reviewer khác. Handbook không tự gửi thông báo hoặc tạo review request hộ thành viên.

Khi có góp ý: đọc và trả lời từng điểm, sửa code, chạy lại kiểm tra liên quan, **commit/push vào cùng nhánh**. PR tự cập nhật, không cần mở PR mới cho mỗi lần sửa. Nêu commit đã xử lý; không đánh dấu resolved chỉ để dọn giao diện khi chưa giải quyết. Nếu không đồng ý, giải thích bằng UC/contract/test và thống nhất với reviewer. Sau code mới, nhờ reviewer rà lại; không lấy approval cũ làm bằng chứng cho thay đổi chưa được xem.

## 7. Reviewer phải xem gì?

Trong **Files changed**, đọc diff và mở đủ ngữ cảnh. Chạy thử hoặc kiểm tra bằng chứng phù hợp độ ảnh hưởng. Có thể lấy nhánh về bằng fetch/switch sau khi giữ an toàn công việc local; không sửa trực tiếp nhánh tác giả nếu chưa thống nhất.

- Khớp task và UC/BR? Có sửa ngoài phạm vi hay đổi contract không báo?
- Request đi vào đâu, service nào xử lý, dữ liệu nào đọc/ghi? Một nhánh lỗi trả gì?
- Input, ngày Việt Nam, cutoff, giá và Segment đúng? Unknown không được coi là FREE; refresh không tạo/gia hạn hold.
- Module chỉ dùng port công bố? Migration có đúng version/ownership và không sửa file đã chia sẻ?
- Tests có ý nghĩa, có nhánh lỗi và lệnh thực chạy? UI có loading/rỗng/lỗi/mobile? Không có secret/build output?

Chọn **Comment** để trao đổi, **Request changes** khi cần sửa, **Approve** khi phần mình đã rà đạt. Comment kiểu “ok” không thay approval. Agent có thể hỗ trợ tìm vấn đề nhưng không thay review độc lập của thành viên. Cách dùng review tham khảo [GitHub Docs](https://docs.github.com/en/pull-requests/reference/pull-request-reviews).

## 8. Khi nào được merge?

Trung Hiếu kiểm tra trên **commit mới nhất**:

1. PR sẵn sàng, không còn Draft; đúng task/contract, một người khác đã approve và không còn yêu cầu sửa chưa giải quyết.
2. `frontend-quality`, `backend-quality`, `team11-ci-gate` đều success; failure/cancelled/skipped/pending không tính đạt. Đọc log bước lỗi, không chỉ xem huy hiệu chung.
3. Không conflict; đã cập nhật main khi cần, chạy lại kiểm tra sau khi ghép; migration merge đúng thứ tự.
4. Tài liệu/demo và kết quả kiểm tra đủ; chọn **Squash and merge** theo quy ước nhóm nếu repo cho phép phương thức này. Nếu không có tùy chọn, báo leader kiểm tra settings thay vì tự đổi settings/bypass.

Việc “leader quản lý merge, một approval và gate” là **quy trình nhóm**. Branch protection/ruleset/required checks và enforcement theo quyền/gói GitHub chưa được xác minh lại trong lần viết handbook này; không mặc định file CI đã bật khóa merge. Dù nút merge bấm được vẫn phải tuân thủ quy trình. Tham khảo [GitHub về merge PR](https://docs.github.com/en/pull-requests/how-tos/merge-pull-requests/merging-a-pull-request).

Sau khi task đã merge và local sạch:

```powershell
git switch main
git pull --ff-only origin main
```

Tạo nhánh mới cho task kế tiếp từ main mới. Không tiếp tục commit trên nhánh PR đã squash/merge. Không xóa branch còn công việc chưa nhập.

## 9. Task phụ thuộc và conflict

**Thiếu port/schema:** vẫn làm unit tests/prototype theo contract; ghi dependency trong PR Draft. Chờ PR prerequisite merge rồi cập nhật main vào nhánh task. Đừng copy repository/entity hoặc cherry-pick code người khác để né dependency; nhánh chồng nhau chỉ dùng khi đã thống nhất với leader.

**Cập nhật main vào nhánh task:** sau khi đã commit hoặc xử lý an toàn thay đổi local, đứng trên nhánh task của mình (không phải main):

```powershell
git status
git branch --show-current
git fetch origin
git merge origin/main
```

Nếu merge thành công: chạy kiểm tra liên quan rồi push nhánh. Nếu conflict: `git status` cho biết file; đọc hai phía, thống nhất với chủ file, sửa đúng nội dung và bỏ markers `<<<<<<<`, `=======`, `>>>>>>>`. Stage đúng file đã giải quyết, `git merge --continue`, chạy kiểm tra rồi push. Không chọn “accept current/incoming all” khi chưa hiểu dữ liệu bị bỏ.

Nếu chưa biết giải quyết và đang có merge dở, có thể `git merge --abort` để dừng lần merge này rồi nhờ hỗ trợ; không chạy lệnh đó nếu thao tác đang làm là rebase. Không hướng dẫn người khác reset/force push để chữa conflict. Khi push bị từ chối vì remote có commit mới, fetch/xem lịch sử và hỏi chủ nhánh, không ép ghi đè.

**V2/V3/V4:** Trung Hiếu I1/V2 → Phương P1/V3 → Huy H1/V4. Chuẩn bị code có thể song song, merge/apply DB dùng chung theo thứ tự. Kiểm tra main/PR trước khi chia sẻ số migration; số trùng báo leader. Seed demo tách schema; không sửa migration đã chia sẻ/áp dụng, bật out-of-order, skip hoặc xóa volume dùng chung để chữa.

## 10. Báo tiến độ và dùng agent

Cuối buổi gửi ngắn theo mẫu; kèm link PR khi đã có:

```text
Task: H1
Đã làm: ...
Đã kiểm tra: lệnh/kết quả ...
Đang chờ: P1/V3 hoặc vấn đề cụ thể ...
Tiếp theo / dự kiến bàn giao: ...
```

Mỗi người cập nhật SAD/weekly phần mình, cung cấp ca kiểm thử và sửa bug của mình. Đức Hiếu tổng hợp/rà chéo; không nhận hết việc tài liệu/test/sửa lỗi. Functional report ghi actual/result thật cho tối thiểu 15 ca, không lấy unit test hay health thay thế.

Nếu dùng agent: giao ID task, UC/BR, contract, file được sửa, tiêu chí nghiệm thu và lệnh kiểm tra; cung cấp AGENTS. Đọc diff, chạy kiểm tra và giải thích được luồng xử lý trước khi gửi PR. Không giao agent đổi stack/security/contract hoặc push/merge thay mình khi chưa được giao quyền đó. Khi vướng điều chưa rõ, báo sớm bằng lỗi/request/expected cụ thể thay vì tự đoán nghiệp vụ.
