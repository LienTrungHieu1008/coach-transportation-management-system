# CI và tích hợp guardrails — Team 11

## Trạng thái hiện tại

Bộ 10 file `PA4_Repo_Guardrails` đã được tích hợp; sau đó đã thêm bộ khung FE/BE, test nền tảng, Maven Wrapper, npm lockfile và Compose theo yêu cầu người dùng. [Báo cáo đối chiếu ban đầu](docs/management/pa4-guardrails-audit.md) giữ trạng thái repo rỗng trước khi dựng; xem [hướng dẫn hiện tại](docs/development.md) và báo cáo kiểm chứng bộ khung để biết kết quả thực tế.

Workflow `.github/workflows/ci.yml` đã push cùng baseline tại PR #1; frontend/backend/gate đã thành công trên GitHub cho commit `efa1d0f`. Xem [bằng chứng và trạng thái bàn giao](docs/management/baseline-publication.md). Branch protection/required check là settings riêng, chưa cấu hình trong phiên. Không xem lint workflow/YAML thành công là build/test ứng dụng thành công. Không dùng scripts giả, `--if-present`, `continue-on-error`, path filters hay điều kiện bỏ job khi thiếu source để đổi kết quả thành xanh.

Source nằm tại `src/frontend/` và `src/backend/`, giữ bố cục `src/` của repo. Nếu đổi vị trí, đồng bộ workflow, AGENTS, contract kiến trúc và CODEOWNERS.example trong cùng PR.

## Điều kiện để chạy kiểm tra ứng dụng

Frontend tại `src/frontend/` cần:

- `package.json`, `package-lock.json` và `.nvmrc` có nội dung thật, được commit. Chọn Node phù hợp với phiên bản Vite/dependencies khi khởi tạo; Node trên máy người tích hợp không phải baseline của dự án.
- `lint` chạy ESLint với cấu hình thực; `typecheck` kiểm tra TypeScript; `build` build Vite thực. Không dùng script chỉ in thông báo hoặc thoát thành công.
- `tsconfig`/project references phù hợp với lệnh typecheck đã chọn; không mặc định script build đã thay thế typecheck.

Backend tại `src/backend/` cần:

- Java 21, `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` và các file bổ sung do loại Maven Wrapper thực tế yêu cầu. Giữ LF cho `mvnw` để chạy trên Linux.
- Maven cấu hình JUnit và ít nhất một test có ý nghĩa. Lệnh `verify -DfailIfNoTests=true` yêu cầu có test; không thêm test rỗng cho đủ số lượng, không cấu hình skip test.
- Nếu có `*IT`, cấu hình Failsafe cho integration-test/verify; `mvn verify` không tự chạy mọi loại test. Không mặc định nó đã chạy ArchUnit, format hay kiểm tra OpenAPI.
- App/test dùng datasource test tạm; Flyway tạo được schema từ DB mới. Tests mock Gemini/payment gateway, không cần khóa Gemini/VNPay thật.
- PostgreSQL 16 đã thống nhất giữa workflow, `compose.yaml` và `compose.test.yaml`. Verify phải dùng DB test riêng; các password CI/Compose mặc định chỉ là giá trị công khai dùng cho test/local.

## Các check

| Check | Thực hiện |
|---|---|
| `frontend-quality` | Bắt thiếu file đầu vào → Node theo `.nvmrc` → `npm ci` → lint → typecheck → Vitest → build |
| `backend-quality` | PostgreSQL tạm → bắt thiếu file đầu vào → Temurin 21 → Maven verify, gồm Surefire + Failsafe, yêu cầu có test |
| `team11-ci-gate` | Chạy với `always()`; chỉ thành công khi cả hai quality jobs có kết quả `success` |

Thiếu file là lỗi, không phải lý do skip. Gate từ chối kết quả `failure`, `skipped`, `cancelled` của dependency; khi cả workflow bị hủy, không coi run đó là thành công. Workflow chạy PR vào `main`, push `main` và chạy thủ công; không deploy, không cần secret dự án, quyền mặc định chỉ đọc contents. Actions được pin full SHA lấy từ tag `v6` ở repo chính thức; cập nhật có review, không bịa SHA.

Ngữ nghĩa `needs`/`always()` tham khảo [GitHub Actions jobs](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-jobs). Cấu hình action theo [checkout](https://github.com/actions/checkout), [setup-node](https://github.com/actions/setup-node), [setup-java](https://github.com/actions/setup-java).

## Chạy local sau khi có source

PowerShell, từ gốc repo, chạy từng khối và kiểm tra exit code từng lệnh:

```powershell
Set-Location src/frontend
npm.cmd ci
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
```

```powershell
Set-Location src/backend
.\mvnw.cmd -B -ntp verify -DfailIfNoTests=true
```

Backend cần PostgreSQL test riêng và cấu hình tương ứng như workflow (`SPRING_DATASOURCE_*`), không trỏ vào DB thật. `ci_only_password` chỉ dùng cho service tạm do workflow tạo. Linux/macOS dùng `npm` và `./mvnw`; kiểm tra Node theo `.nvmrc`, Java 21 và PostgreSQL 16. Nếu host chưa có JDK/DB, dùng `docker compose -f compose.test.yaml run --rm backend-test` như [hướng dẫn phát triển](docs/development.md). Kết quả local và GitHub phải gắn đúng commit/run, không suy ra kết quả của commit mới từ run cũ.

## Bật bảo vệ main sau khi được giao thao tác remote

Đã push và mở draft PR #1; chưa merge, đổi settings hoặc xác minh tính năng bảo vệ theo gói GitHub. Các bước settings bên dưới chỉ làm khi được giao:

1. Push nhánh và mở PR vào `main`; kiểm tra log từng job. Hoàn thiện source/build/test thật để quality jobs thành công, không nới gate để merge bộ khung rỗng.
2. Cấu hình branch protection/ruleset cho `main`: bắt PR, ít nhất một người khác approval, giải quyết conversations, loại approval cũ khi đổi code và yêu cầu nhánh cập nhật. Đặt required check đúng tên `team11-ci-gate`; kiểm tra tính năng thực sự được enforce theo quyền/gói repo. Xem [GitHub protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches).
3. Không cho force push/delete và hạn chế bypass theo khả năng repo. Quy ước Hiếu quản lý merge không tự tạo khóa kỹ thuật khi thành viên vẫn có quyền write.
4. `.github/CODEOWNERS.example` chưa hoạt động: thay placeholder bằng reviewer thật có quyền phù hợp rồi mới tạo `.github/CODEOWNERS`. Không đặt Hiếu làm reviewer duy nhất cho PR do Hiếu viết. Chưa biết reviewer nên không tự kích hoạt.
5. Dùng PR thử có lỗi TypeScript: quality job và gate phải lỗi, merge phải bị chặn. Sửa lỗi thì vẫn phải có approval; push đổi code phải loại approval cũ theo cấu hình. Không merge lỗi thử. Bước này chưa thực hiện.

## Giới hạn và kiểm thử tiếp theo

Đã có base package và health/CSRF API kỹ thuật; chưa có schema/API nghiệp vụ, ArchUnit/OpenAPI/import checks. Review thủ công module/layer và truy vết UC cho đến khi các kiểm tra thực được bổ sung. Ưu tiên test tranh chấp ghế trên Segment giao nhau, hold hết hạn/retry không kéo dài, payment lặp và role/ownership. Test nền tảng chưa phải test plan/kết quả functional testing của 3 UC theo PA4.

Codex đọc AGENTS; CLAUDE và Copilot instructions trỏ về nguồn chung. Agent khác cần được cung cấp AGENTS + contracts + UC liên quan; file hướng dẫn không thay thế CI, review hoặc quyền repo.
