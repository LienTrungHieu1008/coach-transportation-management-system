# Booking — bàn giao H1/H2/H3, UC-P07

Huy phụ trách V4/persistence, logic ghế, H2 availability và H3 cleanup. Java 21,
Spring Boot, PostgreSQL 16, Flyway, JdbcTemplate; không thêm dependency.

## API và quy tắc

`GET /api/v1/trips/{tripId}/seat-availability?fromStopId={uuid}&toStopId={uuid}` công khai,
không cần cookie đăng nhập. ID phải là UUID chuẩn; mỗi query đúng một giá trị.
Chỉ mở đúng GET này; private/write vẫn cần xác thực, request ghi cần CSRF hợp lệ.
Chưa có API tạo Booking, thanh toán hoặc luồng login.

Controller → application → TripQueryPort của Phương → BookingRepository → core.
Một `asOf` UTC tới microsecond, transaction read-only REQUIRED/REPEATABLE_READ,
cùng DataSource/snapshot cho Trip và Booking. GET không giữ ghế hoặc chạy cleanup.

- Segment `[fromIndex, toIndex)`, 0-based; A–B và B–C không giao nhau.
- Confirmed/Confirmed chưa release chặn ghế. Pending/Hold chỉ chặn khi `asOf < expiresAt`.
- Hold 10 phút, refresh không gia hạn; đúng expiry không còn chặn dù cleanup trễ.
- Trả đủ ghế theo layout; FREE/Confirmed blocker có expiry null, chỉ Hold chặn lấy expiry sớm nhất.
- Mọi response của controller có `Cache-Control: no-store`; không lộ hành khách/Booking.

| HTTP | Code |
|---|---|
| 400 | INVALID_REQUEST |
| 404 | TRIP_NOT_FOUND |
| 422 | STOP_NOT_SERVED, INVALID_SEGMENT |
| 409 | TRIP_NOT_BOOKABLE, ESSENTIAL_DATA_INCOMPLETE |
| 503 | AVAILABILITY_UNAVAILABLE |
| 500 | INTERNAL_ERROR |

V4 tạo `booking`, `booking_seat`, `seat_allocation`, phụ thuộc V2/V3; UUID do caller cấp,
migration không seed. Composite FK giữ đúng parent/ghế/Trip/Segment/expiry.
H3 khóa Pending đến hạn bằng `SKIP LOCKED`, chuyển Expired và release đúng Hold chưa release
trong một transaction; idempotent, lỗi rollback, không đổi Confirmed/Cancelled.

| Cấu hình runtime | Mặc định |
|---|---|
| BOOKING_EXPIRY_CLEANUP_ENABLED | true |
| BOOKING_EXPIRY_CLEANUP_INTERVAL | PT5S; phải dương, không quá 5 giây |

## 1. Dựng API/DB demo riêng

Lệnh Bash dưới đây chạy từ gốc repo, cần Docker Compose và curl, không cần JDK/psql trên host.
Chạy verify để tạo JAR; chỉ chạy lại khi source thay đổi:

```bash
docker compose -p team11-huy-public-seat-test -f compose.test.yaml run --rm backend-test
docker compose -p team11-huy-public-seat-test -f compose.test.yaml down
```

Dùng DB tmpfs riêng, không dùng volume phát triển. Port mặc định 18080, có thể đặt
`BOOKING_DEMO_PORT` trước khi chạy. Giữ các biến này trong cùng terminal cho mọi bước demo:

```bash
export COMPOSE_FILE="$PWD/compose.test.yaml"
export COMPOSE_PROJECT_NAME=team11-huy-h2-demo
export BOOKING_DEMO_PORT="${BOOKING_DEMO_PORT:-18080}"
export BOOKING_API_BASE_URL="http://127.0.0.1:$BOOKING_DEMO_PORT"
docker compose up -d --wait db-test
docker compose run -d --rm --name team11-huy-h2-demo-api \
  -p "127.0.0.1:$BOOKING_DEMO_PORT:8080" \
  -e BOOKING_EXPIRY_CLEANUP_ENABLED=false \
  backend-test java -jar target/coach-backend-0.1.0-SNAPSHOT.jar
curl --fail --silent --show-error --retry 30 --retry-connrefused --retry-delay 1 \
  "$BOOKING_API_BASE_URL/api/health"
```

Health UP xác nhận app đã khởi động/migrate V1–V4; chưa chứng minh các ca P07.
Nếu port hoặc tên container đang được dùng, dừng đúng demo cũ hoặc đổi port, không dừng môi trường khác.

## 2. Seed Account → Trip → Booking

Nguồn chung: `src/backend/src/test/resources/fixtures/`.
Giữ nguyên UUID của Phương; Trip runtime dùng ngày mai tại Asia/Ho_Chi_Minh.
Chọn đúng một fixture Booking trên DB sạch: `booking_availability_runtime.sql` (chuẩn),
hoặc `booking_all_unavailable_runtime.sql` (hết ghế). Không nạp chồng hai bộ.

```bash
export BOOKING_FIXTURE=booking_availability_runtime.sql
docker compose cp src/backend/src/test/resources/fixtures db-test:/tmp/booking-fixtures
docker compose exec -T db-test sh -c \
  'psql -X -v ON_ERROR_STOP=1 -1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -f /tmp/booking-fixtures/identity_accounts.sql \
    -f /tmp/booking-fixtures/trip_demo_runtime.sql -f "$1"' \
  sh "/tmp/booking-fixtures/$BOOKING_FIXTURE"
```

Một lệnh, một transaction; SQL/file lỗi phải dừng và rollback toàn bộ. Seed không idempotent.
Bộ chuẩn tạo 2 Account, 2 Trip, 6 physical seats, 5 Booking/seats/allocations.
Booking mẫu chỉ thuộc **Trip ngày** bên dưới; Trip qua đêm không có allocation mẫu.

## 3. Gọi đủ ba Segment

```bash
export TRIP_ID=11111111-1111-4111-8111-111111111111
export STOP_A=22222222-2222-4222-8222-222222222222
export STOP_B=33333333-3333-4333-8333-333333333333
export STOP_C=99999999-9999-4999-8999-999999999999
curl -i --get "$BOOKING_API_BASE_URL/api/v1/trips/$TRIP_ID/seat-availability" \
  --data-urlencode "fromStopId=$STOP_A" --data-urlencode "toStopId=$STOP_B"
curl -i --get "$BOOKING_API_BASE_URL/api/v1/trips/$TRIP_ID/seat-availability" \
  --data-urlencode "fromStopId=$STOP_A" --data-urlencode "toStopId=$STOP_C"
curl -i --get "$BOOKING_API_BASE_URL/api/v1/trips/$TRIP_ID/seat-availability" \
  --data-urlencode "fromStopId=$STOP_B" --data-urlencode "toStopId=$STOP_C"
```

Mỗi response 200, layout `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb`, đúng UUID/thứ tự sáu ghế
trong manifest P1. Expected bộ chuẩn **trước expiry B2**:

| Ghế | Dữ liệu | A→B | A→C | B→C |
|---|---|---|---|---|
| A1 | Không allocation | FREE | FREE | FREE |
| A2 | Confirmed A–B | UNAVAILABLE | UNAVAILABLE | FREE |
| B1 | Confirmed A–C | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE |
| B2 | Hold B–C còn hạn | FREE | UNAVAILABLE | UNAVAILABLE |
| C1 | Hold đã hết hạn | FREE | FREE | FREE |
| C2 | Cancelled/released | FREE | FREE | FREE |

FREE/Confirmed blocker có `expiresAt=null`; B2 trên A→C/B→C có expiry của Hold.

## 4. Thử expiry khi cleanup trễ

Bước 1 đã tắt cleanup. Hold B2 tạo tại `seedTime−8 phút`, hết hạn `seedTime+2 phút`;
`seedTime` là thời điểm bắt đầu transaction seed. Thử ngay sau khi seed, không dùng ngày giờ mẫu cố định.

1. Gọi B→C, ghi `serverTime` và `expiresAt` của B2 (physicalSeatId `77777777-7777-4777-8777-777777777779`).
2. Refresh trước hạn: B2 UNAVAILABLE, expiry giữ nguyên. A→B luôn FREE ở B2.
3. Refresh tại/sau expiry: B2 FREE, expiry null trên A→C/B→C, dù DB vẫn Pending/chưa release.
4. Không cần sửa dữ liệu Hold hoặc tạo Booking qua API. Muốn thử lại phải reset/reseed.

Query chỉ đọc trạng thái B2 bằng psql trong DB container:

```bash
docker compose exec -T db-test sh -c \
  'psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
SELECT b.booking_id, b.status, b.hold_created_at, b.expires_at, a.kind, a.released_at
FROM booking b JOIN seat_allocation a USING (booking_id)
WHERE b.booking_id = 'dddddddd-dddd-4ddd-8ddd-dddddddddd03';
SQL
```

## 5. Bật cleanup và đối chiếu DB

Giữ DB, chỉ thay container API:

```bash
docker stop team11-huy-h2-demo-api
docker compose run -d --rm --name team11-huy-h2-demo-api \
  -p "127.0.0.1:$BOOKING_DEMO_PORT:8080" \
  -e BOOKING_EXPIRY_CLEANUP_ENABLED=true \
  backend-test java -jar target/coach-backend-0.1.0-SNAPSHOT.jar
curl --fail --silent --show-error --retry 30 --retry-connrefused --retry-delay 1 \
  "$BOOKING_API_BASE_URL/api/health"
```

Chạy lại query bước 4; sau lượt job kế tiếp (chu kỳ mặc định PT5S), B2 là Expired,
Hold có `released_at`, GET vẫn FREE. Confirmed/Cancelled không bị đổi, expiry không gia hạn.
GET không kích hoạt job. C1 đã hết hạn cũng có thể được cleanup ngay khi job bật.

## 6. Thử hết ghế

Reset theo bước 7, dựng API/DB lại theo khối demo bước 1, rồi chọn:

```bash
export BOOKING_FIXTURE=booking_all_unavailable_runtime.sql
```

Chạy hai lệnh copy/psql của bước 2, bỏ dòng export chọn bộ chuẩn ở bước đó.

Bộ này có 1 Booking Confirmed A→C, 6 BookingSeat/allocations, mỗi ghế 250.000 VND,
tổng 1.500.000 VND. Gọi ba request bước 3: đều 200, cả sáu ghế UNAVAILABLE, expiry null.
Các ghế bị chặn bởi Confirmed, không phụ thuộc đồng hồ hoặc cleanup. Không nạp bộ hết ghế lên bộ chuẩn.

## 7. Reset và seed lại

Chỉ dừng đúng demo của hướng dẫn này:

```bash
docker stop team11-huy-h2-demo-api
docker compose down
```

DB dùng tmpfs nên bị bỏ cùng container; không thêm `-v`, không xóa volume phát triển.
Chạy lại khối demo bước 1 (không cần verify lại), copy fixtures và seed theo bước 2.
Mỗi DB mới phải copy fixtures lại. Chọn lại bộ chuẩn nếu vừa thử hết ghế.
Trip cũ không tự đổi sang ngày mai; reseed tạo ngày Trip và expiry mới.

## Kiểm chứng và người nhận

Tests ở `src/backend/src/test/java/vn/edu/team11/coach/booking/`; security chung ở
`identity/infrastructure/SecurityConfigurationTests.java`, HTTP Trip ở `trip/api/PublicTripsSecurityIT.java`.
Chạy full backend verify gồm unit + PostgreSQL integration; tests tắt scheduler tự động,
gọi service/job trực tiếp. Tests 503 dùng lỗi truy xuất riêng; không thêm nút giả lỗi vào API.
Verify cuối ngày 18/11/2026: **467 tests PASS** (227 unit, 240 integration), không failure/error/skip.
Runtime HTTP thật đã PASS ba Segment chuẩn, expiry khi cleanup tắt, bật cleanup, ba Segment hết ghế,
private/write và reset/reseed với expiry mới. Request/response và kết quả CI được ghi ở PR bàn giao.

Hoàng đọc bước 1–4 để nối F4; Đức Hiếu dùng cả bộ chuẩn/hết ghế/cleanup/reset cho QA.
Phương rà UUID/layout/Segment, P3 và snapshot; Trung Hiếu rà security và điều phối merge.
Nội dung SAD/weekly Booking: GET chỉ đọc qua port/core; ba bảng V4; H3 ghi transaction riêng,
rollback khi lỗi; không suy dữ liệu thiếu thành FREE. Kết quả backend không thay báo cáo flow FE/QA.

Contract: OpenAPI, TripQueryPort, C1, UC-P07/BR-P07-01–04; manifest tại `docs/test/p1-trip-demo-manifest.md`.
PR #5 đã merge H1/V4/H2/H3 và tests; thay đổi này bàn giao public GET và các bước demo còn lại.
