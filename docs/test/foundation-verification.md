# Kiểm chứng bộ khung — 12/11/2026

## Phạm vi

Thêm tài liệu tham khảo gốc và dựng nền tảng ứng dụng theo SAD, giữ thay đổi guardrails trước đó. Chưa hiện thực UC nghiệp vụ, chưa hoàn thành PA4. Baseline đã push theo yêu cầu tiếp theo của người dùng, chưa merge; xem [bàn giao GitHub](../management/baseline-publication.md). Kế hoạch/phạm vi: [foundation plan](../superpowers/plans/2026-11-12-foundation.md), [PA4](../../pa/PA4/SCOPE.md).

## Môi trường và nguồn

- Host Windows/PowerShell, Node 24.19.0/npm 11.17.0; Docker Desktop Linux engine 29.7.2, PostgreSQL test 16.15.
- FE dùng React 19.3.1, Vite 8.3.8, TypeScript 5.9.3, Tailwind 4.3.5, ESLint 10.14.0, Vitest 5.1.0. Phiên bản trực tiếp pin trong package.json, có lockfile npm.
- BE đã chạy verify trên Java 21.0.12.1/Spring Boot 3.5.16, Maven Wrapper 3.3.4/Maven 3.10.0; PostgreSQL 16.15 thật trong project test riêng.
- Docker mirror trên máy trả EOF/short read khi tải image. Thử registry Docker Hub trực tiếp và public ECR mirror của Docker Official Images, tận dụng cache; không sửa settings Docker và không tắt TLS verification. Các image dùng lại được tag local đúng nội dung đã tải; không thay bằng image giả.
- Tài liệu gốc: 4 PDF sao chép nguyên byte, kiểm tra SHA256 theo [manifest](../reference-manifest.json). Markdown trích theo trang hỗ trợ tra cứu, không thay sơ đồ/bảng gốc.

## Kết quả đã chạy

| Kiểm tra | Kết quả |
|---|---|
| Frontend RED trước hiện thực | 5/5 test thất bại đúng vì template ban đầu chưa có trạng thái kết nối/retry. |
| `npm ci` | Exit 0; 226 package cài từ lockfile, audit 0 vulnerabilities tại thời điểm chạy. |
| `npm run lint` | Exit 0, `--max-warnings 0`. |
| `npm run typecheck` | Exit 0, strict mode và `skipLibCheck: false`. |
| `npm test` | 1 file, 5 test pass: loading, UP, network failure/retry, HTTP error, malformed successful response. |
| `npm run build` | Exit 0; Vite tạo dist thật. |
| Chrome headless desktop/mobile | Đã xem ảnh desktop 1440 px và mobile 390×844 bằng CDP; mobile innerWidth/scrollWidth đều 390, không tràn ngang. Kiểm tra trạng thái dịch vụ chưa khả dụng vì backend chưa chạy ở lần chụp này. |
| Compose config | `docker compose config --quiet`, cấu hình test và base+dev đều exit 0. |
| PostgreSQL test | Project `team11-coach-test` riêng, DB tmpfs không công bố port; healthy, `select version()` xác nhận PostgreSQL 16.15. |
| `docker compose -f compose.test.yaml run --rm backend-test` | Exit 0, `BUILD SUCCESS` ngày 12/11/2026: 5 unit + 3 integration tests, failures/errors/skipped đều 0. Verify dùng `clean verify -DfailIfNoTests=true`; XML Surefire/Failsafe xác nhận đúng số test. |
| Backend integration assertions | Flyway V1 tạo schema mới; health chỉ trả UP; session và attributes được lưu PostgreSQL; cookie HttpOnly/SameSite=Lax; timeout 1800s; cặp cookie/CSRF thực đi qua kiểm tra CSRF nhưng vẫn bị 401 khi chưa xác thực; thiếu CSRF bị 403. |
| Docker frontend image | `docker compose build web` exit 0; trong Linux image cũng chạy npm ci/lint/typecheck/5 tests/build, không dùng dist giả. |
| Docker backend image | `docker compose build backend` exit 0; compile release 21, chạy đủ 5 unit tests, tạo/repackage JAR thật và đóng vào Temurin JRE 21, chạy dưới user 10001. Không dùng skipTests; 3 IT đã chạy ở verify riêng với DB. |
| Docker image và HTTP toàn stack | `docker compose up --build -d --wait --wait-timeout 180` hoàn tất; DB/backend healthy, web phục vụ tại `http://127.0.0.1:8088`. HTTP smoke qua Nginx: HTML/JS 200, health đúng `{"status":"UP"}`, CSRF bootstrap có cookie HttpOnly/SameSite=Lax và no-store; GET bảo vệ 401, POST thiếu/sai CSRF 403, POST có cookie/token hợp lệ nhưng chưa đăng nhập vẫn 401. |
| Chrome toàn stack | Desktop 1440 px và mobile 390×844 đều hiển thị “Kết nối sẵn sàng”; innerWidth/scrollWidth tương ứng 1440/1440 và 390/390. Đã xem ảnh mobile; ảnh/log kiểm tra nằm trong `tmp/foundation/` bị gitignore. |
| Dọn môi trường test | `docker compose -f compose.test.yaml down` exit 0; dừng/xóa container và network test, giữ cache Maven. Stack local cổng 8088 vẫn chạy để xem thử. |
| GitHub Actions/required gate/protection | Baseline `efa1d0f`: frontend-quality/backend-quality/team11-ci-gate đều success trên [run 43254959247](https://github.com/LienTrungHieu1008/coach-transportation-management-system/actions/runs/43254959247). Chưa cấu hình required checks/branch protection hoặc thử chặn merge. |

## Lỗi phát hiện và xử lý

- Ghi file qua PowerShell pipeline ban đầu làm mất dấu tiếng Việt: đã sửa UTF-8, kiểm tra lại bằng test và Chrome. PDF gốc không bị thay đổi.
- Matcher augmentation của jest-dom không khớp kiểu với Vitest 5: bỏ dependency helper không cần thiết, giữ các assertions trạng thái bằng Vitest/Testing Library; không bật skipLibCheck hoặc dùng `any` để che lỗi.
- npm audit phát hiện minimatch trong tooling: áp dụng override đúng phạm vi lên bản vá 11.0.1 theo [GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249), pin dependency và chạy lại npm ci/lint/typecheck/test/build. Không dùng audit fix --force.
- Thử tải JDK portable qua API/asset cũng gặp lỗi mạng và file không đầy đủ; không sử dụng artifact chưa qua checksum, không thay Java 21 bằng JDK cũ để vượt kiểm tra.
- Docker ngừng trước khi lượt IT đầu tiên kết thúc. Sau khi khởi động lại, chạy lại toàn bộ clean verify với cache và DB test tmpfs mới; kết quả thành công ở trên thuộc lượt chạy hoàn chỉnh này.
- Log verify còn warning metadata repository/Mockito agent; không tắt chúng để che lỗi. Chúng không gây fail lượt chạy này; WebMvcTest có account tự sinh chỉ trong test slice, app runtime chính loại UserDetailsServiceAutoConfiguration và không cung cấp login/Basic/form account mặc định.
- Review chỉ đọc bằng agent riêng không phát hiện Critical/Important; đã sửa README frontend và xóa config Oxlint còn sót từ template. Review này không thay approval của thành viên nhóm.

## Giới hạn

Test nền tảng không phải 15 functional test cases cho 3 UC theo PA4. Chưa có đăng ký/đăng nhập, timeout Staff/Admin theo role, Trip/Booking/Payment, test giao dịch/concurrency, ArchUnit hoặc OpenAPI nghiệp vụ. Chưa đọc kiểm toán toàn bộ SAD/UC hoặc nhận feedback PA3/mẫu Moodle. HTTPS/deployment, Gemini/project quota và branch protection vẫn cần kiểm chứng riêng khi được giao.

Lệnh chạy lại và phân biệt local/dev/test nằm ở [development](../development.md). Đây là kết quả local tại thời điểm kiểm tra, không bảo đảm stack tiếp tục chạy sau khi Docker/máy được tắt. Không thay kiểm tra bằng test hoặc script rỗng.
