# Chạy bộ khung local

## Môi trường

- Docker Desktop dùng Linux containers, Docker Compose v2.
- Nếu chạy frontend trên host: Node **24.19.0** theo `src/frontend/.nvmrc`, npm 11.
- Nếu chạy backend trên host: JDK **21**; dùng Maven Wrapper, không cần cài Maven riêng. Phiên dựng bộ khung dùng Docker vì máy chưa có JDK trên PATH.
- Backend: Spring Boot **3.5.16**, Maven Wrapper **3.3.4** tải Maven **3.10.0**. Database **PostgreSQL 16**, cùng major version giữa Compose và CI.

Phiên bản được chọn để hiện thực baseline SAD, không phải phiên bản đã được SAD chỉ định cho mọi dependency. Xem nguồn stack trong [SAD](analysis%20and%20design/PA3_Group11_SAD_v1.3_final.pdf) và dependency cụ thể trong pom/package-lock.

Frontend pin phiên bản trực tiếp và dùng npm lockfile. Override có phạm vi `minimatch@>=7.0.0 <11.0.1` lên `11.0.1` để xử lý [GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249) trong dependency tooling ESLint/typescript-eslint; không tắt audit. Vì đây là thay đổi major transitive, khi nâng tooling phải chạy lại lint/typecheck/test/build và bỏ override khi upstream đã dùng bản vá phù hợp. Không dùng `npm audit fix --force` mù quáng.

## Chạy cả ứng dụng

Từ gốc repo:

```powershell
docker compose up --build -d --wait
docker compose ps
```

Mở `http://localhost:8088`. Nginx phục vụ React và chuyển `/api/` sang backend cùng origin; DB/backend chỉ nằm trong mạng Compose. Health: `http://localhost:8088/api/health`.

Các giá trị mặc định trong Compose và `.env.example` chỉ dùng phát triển local, không phải thông tin bí mật hoặc cấu hình production. Có thể chép `.env.example` thành `.env` để đổi password/port; `.env` bị gitignore. Không dùng DB chứa dữ liệu thật.

```powershell
Copy-Item .env.example .env
docker compose logs backend
docker compose down
```

`down` giữ volume phát triển. Không thêm `-v` trừ khi muốn xóa dữ liệu local và hiểu rõ hậu quả. Không tự sửa migration đã chia sẻ; thêm migration mới.

## Kiểm tra frontend

Để dùng Vite hot reload với backend trong Docker, chạy từ gốc repo ở terminal thứ nhất:

```powershell
docker compose -f compose.yaml -f compose.dev.yaml up --build -d --wait db backend
```

Override dev chỉ mở DB 5432/backend 8080 trên loopback. Sau đó ở terminal thứ hai:

```powershell
Set-Location src/frontend
npm.cmd ci
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
npm.cmd run dev
```

Chạy từng lệnh và xem exit code. Vite dev proxy `/api` đến `http://localhost:8080`; Compose mặc định chỉ công bố web 8088, override dev ở trên mới mở thêm port. Có thể chạy backend trên host với JDK 21 và PostgreSQL test/dev riêng; không dùng override dev trong CI. `npm run preview` chỉ xem static build, không thay thế reverse proxy/API runtime.

## Kiểm tra backend với PostgreSQL riêng

Từ gốc repo, dùng project Compose riêng, DB nằm trên tmpfs và không công bố port:

```powershell
docker compose -f compose.test.yaml run --rm backend-test
docker compose -f compose.test.yaml down
```

`backend-test` chạy Maven Wrapper `clean verify -DfailIfNoTests=true` trên Java 21; Surefire chạy unit/security tests, Failsafe chạy `*IT` với PostgreSQL 16 thật. Maven cache nằm trong volume riêng; database test không dùng volume phát triển. Kết quả XML nằm trong `src/backend/target/surefire-reports` và `failsafe-reports` (gitignored).

Nếu đã có JDK 21 và DB test riêng, đặt `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, rồi chạy từ `src/backend`:

```powershell
.\mvnw.cmd -B -ntp verify -DfailIfNoTests=true
```

Không trỏ kiểm thử vào DB thật. Dockerfile backend chạy `package` gồm unit tests để đóng image; bước `verify` riêng mới chạy cả integration tests. Build image thành công không thay thế verify.

## Session, CSRF và giới hạn hiện tại

I1 đã thêm Flyway V2 cho Account prerequisite: xem [contract PK/constraints/fixture](contracts/identity-account.md) và [kiểm chứng I1](test/i1-account-verification.md). Migration không tạo account mặc định; fixture hai Passenger DISABLED chỉ nạp riêng cho local/test. Schema Account không có nghĩa đã có API đăng ký/đăng nhập. V1 session được giữ nguyên; V3/V4 thuộc task thành viên khác.

- `GET /api/health`: chỉ trạng thái tổng quát; không công bố chi tiết DB, credentials hoặc stack trace.
- `GET /api/session/csrf`: cấp token để SPA gửi lại qua header `X-CSRF-TOKEN`; browser giữ cookie session HttpOnly, SameSite=Lax. Token không phải thông tin đăng nhập.
- Mặc định các API khác yêu cầu authentication. Không có form login, HTTP Basic hoặc tài khoản demo mặc định; UC đăng ký/đăng nhập chưa được hiện thực.
- POST/PUT/PATCH/DELETE phải có CSRF hợp lệ; token không thay thế kiểm tra quyền/ownership trong từng UC.
- Session nền tảng dùng timeout 30 phút. Chính sách Staff/Admin 15 phút cần được hiện thực cùng P02 khi có account/role thật; chưa tuyên bố đã đáp ứng.
- Compose local chạy HTTP loopback nên `SESSION_COOKIE_SECURE=false`. Khi triển khai HTTPS cần Secure cookie và cấu hình proxy/TLS phù hợp; task này chưa deploy.

Chưa có UC nghiệp vụ, dữ liệu chuyến, màn hình prototype PA4 hoặc Gemini/payment gateway. Xem [phạm vi PA4](../pa/PA4/SCOPE.md); không dùng trạng thái health/test nền tảng để báo đã đáp ứng functional tests của 3 UC.
