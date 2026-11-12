# Đối chiếu PA4 Repo Guardrails — 11/11/2026

> Đây là snapshot của lần tích hợp guardrails khi repo còn rỗng. Sau đó người dùng đã giao thêm tài liệu gốc và dựng bộ khung (12/11/2026); xem `docs/development.md` và báo cáo kiểm chứng bộ khung. Các kết quả “thiếu source” dưới đây là lịch sử, không phải mô tả source hiện tại.

## Phạm vi và nguồn

Yêu cầu: đọc handoff, kiểm tra hướng dẫn/Git/source, tích hợp contracts, hướng dẫn agent và CI phù hợp; giữ thay đổi hiện có, báo kết quả thực, chưa push/merge.

- Đã đọc `CODEX_HANDOFF.md` trong Downloads và đủ 10 file của `PA4_Repo_Guardrails` tìm thấy tại `OneDrive/Presonal/ChatGPT/IntroSE/PA4_Repo_Guardrails` (cũng có ZIP cạnh thư mục).
- Handoff/bộ kit được dùng làm bối cảnh và mẫu kỹ thuật. Các bước push, PR, settings trong tài liệu không phải ủy quyền thực hiện ở phiên này.
- Chưa đọc lại PDF SAD/UC/PA4 gốc trong phiên tích hợp; không khẳng định đã kiểm toán yêu cầu môn học hoặc đầy đủ mọi UC/BR.

## Baseline repo thực tế

- Repo: `LienTrungHieu1008/coach-transportation-management-system`, remote `origin` dùng HTTPS.
- Ban đầu ở `main`, HEAD `1cb44f8` (`Initialize course project repository structure`); `git status --porcelain=v1 --untracked-files=all` rỗng, không có thay đổi staged/unstaged/untracked. Chỉ thấy `main` và `origin/main` trong refs local; không fetch source mới.
- Không có `AGENTS.md` ở gốc repo hoặc các thư mục cha trên đường dẫn `D:/HieuLT/`; không có AGENTS lồng trong repo. Chưa có CLAUDE, Copilot instructions hay workflow để ghép.
- Cây Git chỉ có `README.md`, `src/.gitkeep`, các thư mục `docs/{management,requirements,analysis and design,test}` và `pa/PA0` đến `pa/PA5` với `.gitkeep`.
- Không có frontend/backend, `package.json`, lockfile, `.nvmrc`, `pom.xml`, Maven Wrapper, Java package, test, API, Flyway migration hoặc Compose. Vì vậy chưa có source để xác nhận phù hợp SAD.

## Kết quả đối chiếu và tích hợp

| Thành phần bộ kit | Thực tế và xử lý |
|---|---|
| `AGENTS.md`, `CLAUDE.md`, Copilot instructions | Tạo mới từ kit; nguồn quy tắc chung nằm ở AGENTS/contracts/CONTRIBUTING. Bổ sung tình trạng source rỗng, lệnh Windows và đường dẫn dự kiến. |
| Kiến trúc | Giữ baseline React/TS/Vite/Tailwind, Java 21/Spring Boot/Maven, modular monolith/PostgreSQL, session JDBC/CSRF, gateway AI chỉ đọc. Đánh dấu đây là baseline được bàn giao, chưa có implementation để chứng minh. |
| Data/API | Giữ quy tắc UUID, UTC/múi giờ, tiền, hold/booking/payment/cancel; bổ sung cutoff 30 phút tại Stop đầu tuyến, redirect không chứng minh thanh toán và hoàn 100% khi nhà xe hủy chuyến từ handoff. Không tạo endpoint/OpenAPI/package giả. |
| Bố cục | Giữ `src/` hiện có; đổi giả định FE/BE ở gốc thành **đường dẫn dự kiến** `src/frontend/`, `src/backend/` trong CI, AGENTS, kiến trúc và CODEOWNERS.example. Không tạo source hoặc di chuyển thư mục cũ. |
| CI | Giữ lint/typecheck/build thật và Maven verify yêu cầu test. Thêm kiểm tra file không tồn tại/rỗng với thông báo lỗi rõ; giữ gate chỉ chấp nhận cả hai job success. Không skip/ignore lỗi để xanh. |
| Actions | Pin full SHA của checkout/setup-node/setup-java đã lấy trực tiếp bằng `git ls-remote` từ tag `v6` ở repo chính thức. Quyền contents read; checkout không lưu credentials. |
| PostgreSQL | `postgres:16` kế thừa kit, đánh dấu tạm vì chưa có Compose. Java 21 là baseline bàn giao; Node, Spring Boot và Maven chưa có phiên bản thực để xác nhận. |
| Đóng góp và review | Giữ CONTRIBUTING và PR template; tác giả chịu trách nhiệm code AI và phải giải thích request, dữ liệu, transaction, một nhánh lỗi. |
| CODEOWNERS | Giữ file `.example`, cập nhật đường dẫn migration và thêm CONTRIBUTING/SETUP_CI. Chưa kích hoạt vì chưa có reviewer thật. |
| README | Chỉ nối thêm phần tiếp nhận PA4 và liên kết tài liệu, giữ nội dung có sẵn. |

Thực hiện trên nhánh local mới `chore/repo-guardrails`. Chưa commit, push, merge, deploy, mở PR hoặc đổi repository settings. Các `.gitkeep` ban đầu được giữ nguyên.

## Kiểm chứng local

Môi trường: Windows/PowerShell, Git 2.35.1.windows.2, Python 3.11 + PyYAML, Git Bash. Node local `v24.19.0`, npm `11.17.0` chỉ được kiểm tra phiên bản, không được chọn làm phiên bản dự án. Không tìm thấy Java/Maven trên PATH; repo cũng chưa có wrapper. Docker CLI hiện có nhưng chưa khởi động DB/test service.

| Kiểm tra đã chạy | Kết quả và giới hạn |
|---|---|
| `git status`, `git remote -v`, `git branch --all`, `git ls-tree -r --name-only HEAD`, `rg --files --hidden -g '!.git'` | Xác nhận baseline và thiếu source như trên. |
| `git ls-remote` trên 3 repo actions, tag `v6` | Xác minh SHA, không thay đổi remote dự án. |
| Parse `.github/workflows/ci.yml` bằng PyYAML BaseLoader; assertions triggers/permissions/paths/SHA/needs/always | Thành công; dùng BaseLoader để không diễn giải khóa `on` thành boolean theo YAML 1.1. |
| `actionlint -color .github/workflows/ci.yml`, bản 1.7.12 Windows amd64 | Exit 0, không có diagnostics. Công cụ tải vào thư mục temp, SHA256 ZIP khớp checksum của release chính thức; không thêm dependency vào repo. Không thay thế chạy Actions trên GitHub. |
| Chạy nguyên văn hai đoạn `Require real ... build inputs` trong workflow bằng Git Bash `-e -o pipefail` | Cả hai exit 1 đúng thực trạng: FE thiếu 3 file, BE thiếu 4 file; đây là lỗi đầu vào ứng dụng, không phải build pass. |
| Chạy nguyên văn đoạn gate với 16 tổ hợp `success/failure/skipped/cancelled` của FE/BE | 16/16 khớp kỳ vọng, chỉ `success/success` exit 0. Kiểm tra logic shell local, chưa kiểm chứng scheduling/cancellation thực trên GitHub. |
| `bash -n` trên cả 9 khối `run` trong workflow | Tất cả hợp lệ về cú pháp Bash. Chưa chạy ShellCheck riêng vì không có công cụ trên PATH. |
| `git diff --check`; `git diff --cached --check` với index tạm chứa cả 13 file thay đổi/mới | Thành công, không lỗi whitespace. Index thật giữ nguyên (đối chiếu SHA256); không stage thay đổi vào index của người dùng. |
| Kiểm tra UTF-8, liên kết Markdown local và nội dung baseline | 8 liên kết local tồn tại trong 11 tài liệu Markdown; các dòng README ban đầu giữ nguyên, cả 11 `.gitkeep` giữ nguyên từng byte. |
| Một lượt review chỉ đọc bằng agent riêng theo skill requesting-code-review | Không có phát hiện đáng kể. Review agent không thay thế approval của thành viên nhóm trên PR. |

Các file đầu vào được báo thiếu:

```text
src/frontend/package.json
src/frontend/package-lock.json
src/frontend/.nvmrc
src/backend/pom.xml
src/backend/mvnw
src/backend/mvnw.cmd
src/backend/.mvn/wrapper/maven-wrapper.properties
```

## Chưa kiểm chứng và việc tiếp theo

1. Chưa chạy npm lint/typecheck/build hoặc Maven verify vì không có ứng dụng/build config; chưa có test nghiệp vụ, test DB/Flyway hoặc kiểm tra giao dịch/concurrency. Không có kết luận ứng dụng chạy được.
2. Chưa chạy GitHub Actions, PostgreSQL service, branch protection/required gate hoặc PR cố ý lỗi. Hướng dẫn thao tác sau nằm ở [SETUP_CI.md](../../SETUP_CI.md); không coi các hướng dẫn này là settings đã được bật.
3. Chưa có ArchUnit, OpenAPI, kiểm tra ranh giới import hoặc test security. Chỉ thêm khi có package/API thật, không tạo check hình thức.
4. Cần nhận/khởi tạo bộ khung source đúng baseline và chốt phiên bản Node/Spring Boot/Maven/PostgreSQL; kiểm tra lại mọi đường dẫn và đồng bộ Compose với CI.
5. Cần chọn UC và đưa tài liệu UC/BR gốc vào context trước khi hiện thực. Ưu tiên test trùng ghế trên Segment giao nhau, hold hết hạn/retry, payment lặp, role/ownership; báo kết quả thực thi PA4 theo môi trường thật.
6. Cần reviewer thật và quyền repo phù hợp trước khi kích hoạt CODEOWNERS/bảo vệ main. Không merge bộ khung rỗng bằng cách nới CI.
