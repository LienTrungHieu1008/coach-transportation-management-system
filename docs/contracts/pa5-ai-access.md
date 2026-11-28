# PA5 — Kiểm tra truy cập Gemini, chốt dữ liệu đầu vào, xử lý lỗi và cấu hình ngoài repo (`CTMS-26-PA5-AI-ACCESS`)

- **Hạng mục Jira:** `CTMS-26-PA5-AI-ACCESS` (mốc bàn giao **29/11/2026**, giờ Việt Nam).
- **Owner thực hiện:** Khánh Hoàng.
- **Phụ thuộc & phối hợp:** Minh Phương (nguồn Trip / `TripQueryPort`), Nhật Huy (nguồn Booking / `BookingQueryPort`), Liên Trung Hiếu (cấu hình, quyền, `RequesterContext`, duyệt FAQ và review).
- **Tài liệu căn cứ:** `pa/PA5/C1_DECISIONS.md` (bản chốt PA5 chưa đồng bộ vào repo), [UC Spec v1.8](../requirements/Team%2011%20-%20rup_ucspec-v2_final.extracted.md) (`UC-P11` trang 23–25, `UC-P12` trang 26–28), [SAD v1.3](../analysis%20and%20design/PA3_Group11_SAD_v1.3_final.extracted.md) (mục 4.5, trang 31–34), [SAD PA4 v2.0](../analysis%20and%20design/PA4_Group11_SAD_v2.0.md) (mục 4.5), [Contract kiến trúc](architecture.md), [Contract dữ liệu & API](data-and-api.md).

---

## 1. Phạm vi và kiến trúc truy cập Gemini qua Backend

1. **Nguyên tắc cô lập & không gọi từ trình duyệt:**
   - Trình duyệt (`src/frontend/`) **tuyệt đối không** gọi trực tiếp Google Gemini Developer API và không giữ API key.
   - Mọi tương tác AI đi qua backend tại module `vn.edu.team11.coach.ai` thông qua giao diện `LlmGateway` và adapter `GeminiGateway` (`POST {baseUrl}/v1beta/models/{model}:generateContent`).
   - API key được gửi qua HTTP header `x-goog-api-key`, **không gắn `?key=` vào URL** để tránh lộ key trong URI hoặc access log.
   - Không gọi `GeminiGateway` khi đang giữ khóa hoặc transaction ghi DB; không lưu lịch sử hội thoại AI vào CSDL nghiệp vụ (`BR-P11-07`, `BR-P12-06`).
2. **Phạm vi triển khai trong ticket này:**
   - Hoàn thiện phần nền tảng `ai` (`InputSanitizer`, `FallbackReason`, `LlmGateway`, `AiAccessErrorCode`, `AiProviderAccessException`, `AiProperties`, `GeminiGateway`, `AiConfiguration`), cấu hình ngoài repo (`.env.example`, `compose.yaml`, `application.yaml`), unit/integration tests và công cụ kiểm chứng truy cập thực tế (`GeminiAccessProbeRunner`).
   - **Chưa mở endpoint HTTP công khai hoặc sửa `SecurityConfiguration.java`, `trip`, `booking`, migration `V1..V4`**, chờ các owner chốt port và chuẩn envelope theo mục 2 và mục 3.

---

## 2. Chốt dữ liệu được dùng và dữ liệu bị loại / cấm gửi

### 2.1. Dữ liệu AI đọc nội bộ qua Port (phần cần chốt với Phương, Huy, Trung Hiếu)

AI Assistance **không sở hữu** dữ liệu Trip, Booking, Account hay chính sách và **không** thực hiện thao tác ghi (tạo Booking, giữ ghế, thanh toán, hủy vé, check-in).

| Nguồn | Owner phụ trách | Port / Nguồn đọc | Dữ liệu được phép đọc | Trạng thái hiện tại & đầu việc chốt tiếp |
|---|---|---|---|---|
| Trip & Operations | Minh Phương | `TripQueryPort` (`trip.contract`) | - `getSearchCatalog()`: danh mục Stop đang phục vụ và loại xe hỗ trợ.<br>- `findSellableTrips(criteria, limit=5)`: tối đa 5 chuyến `Scheduled`, trước `bookingClosesAt` (giờ rời Stop đầu − 30 phút), ngày lọc theo `Asia/Ho_Chi_Minh` tại Stop lên, sắp theo giờ lên tăng dần, kèm giá đoạn hợp lệ.<br>- `getPublicTripFacts(ref)`: thông tin công khai của 1 Trip xác định (`UC-P12 A8`). | [TripQueryPort.java](../../src/backend/src/main/java/vn/edu/team11/coach/trip/contract/TripQueryPort.java) hiện mới có `getSeatAvailabilityFacts(...)` cho P07. Phương cần bổ sung 3 method/DTO trên vào `trip.contract`. |
| Booking & Payment | Nhật Huy | `BookingQueryPort` (`booking.contract`) | Duy nhất `getOwnedBookingFacts(ref, ctx: RequesterContext)` cho nhánh `UC-P12 A9`, trả về `BookingFacts` gồm: `bookingReference`, `journey`, `seats [1..6]`, `tickets [0..6]`, `bookingStatus`, `paymentSummary`, `refundStatus`, `reconciliationStatus`. | Chưa tạo `booking.contract` / `BookingQueryPort`. Huy cần tạo interface + kiểm tra ownership chặt bên trong trước khi trả về. Không gọi lookup của Staff hay `BookingOperationsPort`. |
| Identity & Access | Liên Trung Hiếu | `RequesterContext` | `accountId`, `sessionId`, `role`, `authenticated`, `anonymousConversationId` (nếu chưa đăng nhập) do server dựng từ Spring Session. Không tin `accountId`/`role` do client gửi. | Chờ đồng bộ cùng P01–P03 để tách luồng ẩn danh (P11, P12 FAQ/Trip công khai) và luồng đã đăng nhập Passenger (P12 nhánh Booking). |
| Chính sách FAQ | Liên Trung Hiếu | `FaqRepository` + `SupportContactConfig` | 5 chủ đề được duyệt (`booking`, `seat_hold`, `payment`, `cancellation_refund`, `check_in`), mỗi mục có `version`, `sourceRef` (UC/BR) và `approvalStatus = Approved`. | Cần Trung Hiếu duyệt nội dung bộ FAQ mẫu trước khi ghép `CustomerSupportService`. `SUPPORT_CONTACT_CHANNEL` mặc định để trống nếu chưa có kênh chính thức. |

### 2.2. Ranh giới dữ liệu gửi lên Gemini Developer API (Free Tier)

Theo QĐ-07 (`BR-P11-05`, `BR-P12-06`) và SAD mục 4.5.3, **Gemini Free Tier có thể sử dụng input/output để cải thiện sản phẩm theo điều khoản Google** (không có cam kết zero-retention). Vì vậy, hệ thống áp dụng kiểm soát nghiêm ngặt trước khi gọi `GeminiGateway`:

| Phân loại | Dữ liệu cụ thể | Quy tắc xử lý |
|---|---|---|
| **ĐƯỢC PHÉP gửi lên Gemini** | - Câu hỏi / mô tả hành trình tiếng Việt của người dùng **sau khi qua `InputSanitizer`** (độ dài 1–500 ký tự, chuẩn Unicode NFC).<br>- Danh mục khung công khai (danh sách tên Stop/loại xe hợp lệ để trích xuất `CriteriaDraft`, danh sách chủ đề FAQ để phân loại `SupportIntent`).<br>- Nội dung FAQ đã được duyệt (`approvalStatus = Approved`) để diễn đạt câu trả lời chính sách (`composePolicyAnswer`). | Chỉ gửi phần văn bản đã chuẩn hóa và ngữ cảnh công khai cần thiết; `LlmGateway` chỉ nhận `SafeAiInput` đã được `InputSanitizer` xác nhận. |
| **KHÔNG ĐƯỢC gửi lên Gemini (hiển thị trực tiếp ở Backend/UI)** | - **`BookingFacts`** từ `BookingQueryPort.getOwnedBookingFacts` (trạng thái Booking, ghế, vé, thanh toán, hoàn tiền, đối soát).<br>- **Dữ liệu định danh / PII:** họ tên hành khách (`fullName`, `travellerName`), email, số điện thoại (`phone`), `accountId`, `sessionId`, CSRF token, mật khẩu / `passwordHash`.<br>- **Bí mật hệ thống & thanh toán:** `GEMINI_API_KEY`, thông số/checksum VNPay Sandbox, stack trace, SQL. | - Nhánh `UC-P12 A9` đọc `BookingFacts` qua `BookingQueryPort` và **render trực tiếp** trên UI, tuyệt đối không nhét vào prompt Gemini.<br>- `InputSanitizer` từ chối ngay input chứa email, số điện thoại Việt Nam (`0...`, `84...`, `+84...`), ký tự điều khiển, `<script>` hoặc mẫu prompt injection với `FallbackReason.UNSAFE_INPUT`. |

---

## 3. Đường xử lý khi Provider lỗi, hạn mức và Fallback UI

### 3.1. Nguyên tắc vận hành khi gọi Provider
- **Ngân sách thời gian 10 giây:** Toàn bộ request AI bị giới hạn tối đa `PT10S` (`deadline = now + 10s`). `GeminiGateway` tính `effectiveTimeout = min(configuredTimeout, deadline - now)` và **không tự động retry** khi lỗi hoặc quá hạn (`BR-P11-06`, `BR-P12-06`).
- **Không tự nâng cấp gói trả phí:** Khi hết quota Free Tier, trả fallback, không tự chuyển sang endpoint trả phí (`BR-P11-05`).
- **Hạn mức ứng dụng dùng chung P11/P12 (cấp nguyên tử):** `5 yêu cầu / 60 giây trượt` cho mỗi account hoặc phiên ẩn danh, và `200 yêu cầu / ngày` (`Asia/Ho_Chi_Minh`) toàn hệ thống demo.
- **Không tự tạo dữ liệu giả:** Lỗi truy xuất `TripQueryPort`/`BookingQueryPort` hoặc lỗi Gemini tuyệt đối không trả chuyến xe/giá/chính sách bịa đặt.

### 3.2. Bảng ánh xạ lỗi Provider / Đầu vào / Nguồn dữ liệu → Fallback & HTTP

| Tình huống | Mã nội bộ (`AiAccessErrorCode` / Sanitizer / Nguồn) | `FallbackReason` (SAD 4.5) | HTTP dự kiến | Phương án tiếp tục trên UI |
|---|---|---|---|---|
| Chưa cấu hình `GEMINI_API_KEY` | `MISSING_API_KEY` | `SERVICE_UNAVAILABLE` | `503` | Báo trợ lý AI tạm thời không khả dụng; gợi ý **UC-P04** (P11) hoặc **UC-P04 / UC-P06** + kênh liên hệ cấu hình (P12). |
| Key sai / hết hiệu lực / bị Google từ chối quyền (`401`, `403`, `400 API_KEY_INVALID`) | `PROVIDER_AUTH_DENIED` | `SERVICE_UNAVAILABLE` | `503` | Như trên; log `WARN` ghi rõ `upstreamHttpStatus` + `upstreamStatus` (không lộ API key). |
| Tên model sai / request provider sai (`400` khác, `404`) | `PROVIDER_BAD_REQUEST` | `SERVICE_UNAVAILABLE` | `503` | Như trên. |
| Hết quota provider (`429 RESOURCE_EXHAUSTED`) hoặc chạm trần app (5/phút, 200/ngày) | `PROVIDER_QUOTA_EXCEEDED` / `AiRequestLimiter` | `LIMIT_REACHED` | `429` | Báo tạm chạm giới hạn (kèm `retryAfterSeconds` khi đến từ limiter ứng dụng) và gợi ý **UC-P04** / **UC-P06**. |
| Quá hạn 10 giây hoặc timeout mạng (`HttpTimeoutException`) | `PROVIDER_TIMEOUT` | `TIMEOUT` | `503` | Báo quá thời gian chờ và gợi ý **UC-P04** / **UC-P06**. |
| Lỗi mạng, TLS, DNS hoặc Google `5xx` | `PROVIDER_UNAVAILABLE` | `SERVICE_UNAVAILABLE` | `503` | Báo dịch vụ tạm gián đoạn và gợi ý **UC-P04** / **UC-P06**. |
| Response `200` nhưng bị chặn safety (`promptFeedback.blockReason`), rỗng hoặc JSON lỗi | `PROVIDER_INVALID_RESPONSE` | `SERVICE_UNAVAILABLE` | `503` | Báo không thể xử lý phản hồi AI và gợi ý **UC-P04** / **UC-P06**. |
| Input rỗng, quá 500 ký tự, có PII (email/SĐT), ký tự điều khiển hoặc injection | `InputSanitizer.Rejected` | `UNSAFE_INPUT` | `422` / `400` (chờ chốt) | Giải thích lý do từ chối an toàn (không gửi PII lên AI), giữ người dùng chỉnh lại câu hỏi hoặc chuyển sang form chuẩn **UC-P04**. |
| Hỏi Booking khi chưa đăng nhập | Auth filter / Service | `LOGIN_REQUIRED` | `401` | Mời đăng nhập qua **UC-P02**; không tiết lộ thông tin Booking. |
| Booking không tồn tại hoặc không thuộc người gọi | `BookingQueryPort` | `BOOKING_INACCESSIBLE` | `404` | Báo không tìm thấy Booking thuộc quyền truy cập (không xác nhận Booking của người khác có tồn tại). |

> [!IMPORTANT]
> **Điểm cần Trung Hiếu chốt trước khi mở HTTP endpoint P11/P12:** Thống nhất cấu trúc JSON trả về cho HTTP `429` / `503` giữa format lỗi chung OpenAPI (`{code, message, fieldErrors}`) và `FallbackResponse` trong SAD (`requestId`, `status`, `reason`, `suggestedUseCase`, `retryAfterSeconds`) để FE (`shared/api/`) ánh xạ nhất quán.

---

## 4. Danh sách biến cấu hình ngoài repo

Mọi giá trị nhạy cảm hoặc thay đổi theo môi trường được truyền qua biến môi trường hoặc file `.env` cục bộ (đã nằm trong `.gitignore`), **không commit secret vào Git**.

| Biến môi trường | Khóa Spring (`application.yaml`) | Giá trị mặc định (an toàn) | Bắt buộc / Ràng buộc |
|---|---|---|---|
| `GEMINI_API_KEY` | `ai.gemini.api-key` | `""` (rỗng) | Bắt buộc có key thật khi chạy tính năng AI thật; nếu rỗng, backend vẫn khởi động bình thường và `GeminiGateway` trả `MISSING_API_KEY`. |
| `GEMINI_MODEL` | `ai.gemini.model` | `gemini-2.5-flash` | Khớp `[A-Za-z0-9._-]+`; cấu hình model Free Tier theo QĐ-07. |
| `GEMINI_BASE_URL` | `ai.gemini.base-url` | `https://generativelanguage.googleapis.com` | URI `http`/`https` hợp lệ có host; cho phép trỏ mock server khi kiểm thử. |
| `GEMINI_TIMEOUT` | `ai.gemini.timeout` | `PT10S` | ISO-8601 `Duration`, phải dương và `<= PT10S` (đúng trần 10 giây của `BR-P11-06`/`BR-P12-06`). |
| `AI_QUOTA_PER_MINUTE` | `ai.quota.per-minute` | `5` | Số nguyên dương (`> 0`). |
| `AI_QUOTA_DAILY_DEMO` | `ai.quota.daily-demo` | `200` | Số nguyên dương (`> 0`). |
| `SUPPORT_CONTACT_CHANNEL` | `ai.support.contact-channel` | `""` (rỗng) | Giữ trống theo SAD 4.5 khi chưa cấu hình kênh hỗ trợ chính thức. |

Các file đã khai báo cấu hình tương ứng:
- [`.env.example`](../../.env.example): mẫu biến môi trường công khai (không chứa key thật).
- [`compose.yaml`](../../compose.yaml): truyền các biến trên vào service `backend` với fallback mặc định rỗng/an toàn.
- [`application.yaml`](../../src/backend/src/main/resources/application.yaml): bind thuộc tính dưới prefix `ai.*`.

---

## 5. Minh chứng kiểm tra thực tế

Thực thi ngày **28/11/2026** (giờ Việt Nam) trên nhánh `CTMS-26-PA5-AI-ACCESS` trong Docker Compose tách riêng (`compose.test.yaml`, Java `21.0.12.1`, PostgreSQL `16.16` trên tmpfs, Docker Engine `29.9.3`).

### 5.1. Kiểm thử tự động toàn Backend (`Surefire` + `Failsafe`)

Do môi trường Windows bind mount giữ handle thư mục rỗng ở `target/test-classes` khi chạy `clean` từ container, lệnh thực tế đã chạy:

```powershell
docker compose -f compose.test.yaml run --rm backend-test sh ./mvnw -B -ntp clean verify '-Dmaven.clean.failOnError=false' '-DfailIfNoTests=true'
```

- **Kết quả:** `BUILD SUCCESS`, exit code `0` (hoàn tất `2026-11-28T03:24:46Z`, tức `10:24:46` giờ Việt Nam).
- **Tổng hợp XML reports:**
  - **Surefire (Unit tests):** `317` tests run, `0` failures, `0` errors, `0` skipped — trong đó có `18` unit tests mới cho AI:
    - `InputSanitizerTest`: `6/6 PASS` (kiểm tra chuẩn hóa câu tiếng Việt, ngày/giờ/giá/UUID/BK ref, chặn email/SĐT VN/ký tự điều khiển/injection).
    - `AiPropertiesTest`: `3/3 PASS` (kiểm tra default rỗng, ràng buộc timeout `<= PT10S`, chặn path traversal ở tên model, quota `> 0`).
    - `GeminiGatewayTest`: `9/9 PASS` (mock HTTP server cục bộ: `MISSING_API_KEY`, `200 OK` gửi key qua header `x-goog-api-key` không gắn query string, `403 PERMISSION_DENIED`, `400 API_KEY_INVALID`, `404 NOT_FOUND`, `429 RESOURCE_EXHAUSTED` → `LIMIT_REACHED`, `503 UNAVAILABLE`, timeout/deadline hết hạn với đúng 1 request không retry, safety block `200`).
  - **Failsafe (PostgreSQL 16 Integration tests):** `289` tests run, `0` failures, `0` errors, `0` skipped — trong đó có `1` integration test mới:
    - `GeminiGatewayContextIT`: `1/1 PASS` (Spring Boot context + Flyway V1–V4 khởi động bình thường khi `GEMINI_API_KEY` rỗng, `/api/health` trả `200`, `LlmGateway.generateText(...)` trả lỗi rõ ràng `MISSING_API_KEY` / `SERVICE_UNAVAILABLE`).
  - **Tổng cộng:** **606/606 PASS**, `0 failures`, `0 errors`, `0 skipped`.

### 5.2. Probe truy cập Gemini Developer API thực tế từ Backend

Chạy `vn.edu.team11.coach.ai.GeminiAccessProbeRunner` từ container `backend-test` (Java 21):

```powershell
docker compose -f compose.test.yaml run --rm --no-deps backend-test sh -c './mvnw -B -ntp dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && CP=target/classes:target/test-classes:$(cat /tmp/cp.txt) && java -cp $CP vn.edu.team11.coach.ai.GeminiAccessProbeRunner && GEMINI_API_KEY=UNCONFIGURED_PROBE_KEY java -cp $CP vn.edu.team11.coach.ai.GeminiAccessProbeRunner'
```

- **Lượt 1 — Chưa cấu hình `GEMINI_API_KEY` ngoài repo:**
  - Backend không mở kết nối mạng và ghi nhận lỗi truy cập rõ ràng:
    ```text
    03:26:46.845 [main] WARN vn.edu.team11.coach.ai.infrastructure.GeminiGateway -- Gemini access failed: errorCode=MISSING_API_KEY fallbackReason=SERVICE_UNAVAILABLE model=gemini-2.5-flash upstreamHttpStatus=null upstreamStatus=null latencyMs=0
    PROBE_RESULT outcome=ACCESS_ERROR timestamp=2026-11-28T03:26:46.795434877Z baseUrl=https://generativelanguage.googleapis.com model=gemini-2.5-flash apiKeyConfigured=false errorCode=MISSING_API_KEY fallbackReason=SERVICE_UNAVAILABLE upstreamHttpStatus=null upstreamStatus=null message="Gemini API key is not configured."
    ```
- **Lượt 2 — Kiểm tra kết nối HTTPS thật từ Backend tới `https://generativelanguage.googleapis.com` (với sentinel key thử `UNCONFIGURED_PROBE_KEY` do máy chưa có `.env` / key Free Tier thật):**
  - Backend kết nối TLS/HTTPS thành công tới Google (`latencyMs=866`); Google phản hồi `HTTP 400` (`INVALID_ARGUMENT` / `API_KEY_INVALID`) và `GeminiGateway` ánh xạ chuẩn sang `PROVIDER_AUTH_DENIED` (`SERVICE_UNAVAILABLE`), không lộ API key trong log:
    ```text
    03:26:49.894 [main] WARN vn.edu.team11.coach.ai.infrastructure.GeminiGateway -- Gemini access failed: errorCode=PROVIDER_AUTH_DENIED fallbackReason=SERVICE_UNAVAILABLE model=gemini-2.5-flash upstreamHttpStatus=400 upstreamStatus=INVALID_ARGUMENT latencyMs=866
    PROBE_RESULT outcome=ACCESS_ERROR timestamp=2026-11-28T03:26:48.961483860Z baseUrl=https://generativelanguage.googleapis.com model=gemini-2.5-flash apiKeyConfigured=true errorCode=PROVIDER_AUTH_DENIED fallbackReason=SERVICE_UNAVAILABLE upstreamHttpStatus=400 upstreamStatus=INVALID_ARGUMENT message="Gemini API rejected authentication or project access."
    ```

### 5.3. Cách chạy lại khi có `GEMINI_API_KEY` thật (ngoài repo)

1. Sao chép `.env.example` thành `.env` tại gốc repo (`.env` đã nằm trong `.gitignore`) và điền `GEMINI_API_KEY=...` từ Google AI Studio.
2. Nạp `.env` trong shell hiện tại (không commit key) và chạy probe để kiểm chứng `outcome=SUCCESS` (`200 OK`):

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*GEMINI_API_KEY=' } | ForEach-Object { $env:GEMINI_API_KEY = ($_ -split '=', 2)[1].Trim() }
docker compose -f compose.test.yaml run --rm --no-deps -e GEMINI_API_KEY backend-test sh -c './mvnw -B -ntp test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && CP=target/classes:target/test-classes:$(cat /tmp/cp.txt) && java -cp $CP vn.edu.team11.coach.ai.GeminiAccessProbeRunner'
docker compose -f compose.test.yaml down
```

- Đã chạy `docker compose -f compose.test.yaml down` (exit code `0`, không `-v`) sau khi kiểm chứng.
