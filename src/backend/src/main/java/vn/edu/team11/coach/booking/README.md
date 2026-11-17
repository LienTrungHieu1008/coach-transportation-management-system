# Booking — UC-P07

Phần Nhật Huy phụ trách: H1 logic ghế và persistence V4, H2 API availability, H3 dọn hold hết hạn.
Dùng Java 21, Spring Boot, PostgreSQL 16, Flyway và JdbcTemplate; không thêm dependency.

## Quy tắc nghiệp vụ

- Segment là khoảng nửa mở `[fromIndex, toIndex)`, index 0-based. A–B và B–C dùng lại cùng ghế được.
- Allocation chưa release chỉ chặn khi Booking/Kind là Confirmed/Confirmed, hoặc Pending/Hold còn hạn.
- Hold có thời hạn 10 phút; tại đúng expiry không còn chặn, kể cả cleanup chưa chạy.
- Trả đủ ghế theo thứ tự layout. FREE có expiry null; Confirmed chặn có expiry null; chỉ Hold chặn thì lấy expiry sớm nhất.

## Luồng API

```http
GET /api/v1/trips/{tripId}/seat-availability?fromStopId={uuid}&toStopId={uuid}
```

Ba ID phải là UUID đầy đủ, chấp nhận hoa/thường; mỗi query bắt buộc đúng một giá trị.
Controller → application → TripQueryPort của Phương → BookingRepository → core domain.
Application lấy một thời điểm UTC, cắt tới microsecond; Trip facts và allocations dùng cùng transaction
read-only REQUIRED/REPEATABLE_READ. Caller đã có transaction cũng phải giữ REPEATABLE_READ.

Response gồm `tripId`, hai Stop ID, `serverTime`, `bookingClosesAt`, `layout` và `seats`.
Hết ghế vẫn 200; thành công/lỗi qua controller đều `Cache-Control: no-store`.
Không tạo hold, chạy cleanup, lộ thông tin hành khách hoặc suy lỗi dữ liệu thành FREE.

| HTTP | Code |
|---|---|
| 400 | INVALID_REQUEST |
| 404 | TRIP_NOT_FOUND |
| 422 | STOP_NOT_SERVED, INVALID_SEGMENT |
| 409 | TRIP_NOT_BOOKABLE, ESSENTIAL_DATA_INCOMPLETE |
| 503 | AVAILABILITY_UNAVAILABLE |
| 500 | INTERNAL_ERROR |

## Persistence và cleanup

`V4__booking_availability.sql` tạo `booking`, `booking_seat`, `seat_allocation`, phụ thuộc V2/V3.
UUID do caller cấp; migration không seed. Composite FK giữ đúng Booking/ghế/Trip/Segment/expiry.
Booking chỉ truy vấn bảng của mình; Trip/layout/Stop dùng port công bố, giữ nguyên UUID của Phương.

Scheduler gọi service transaction riêng: khóa Pending đến hạn theo Booking ID với `SKIP LOCKED`,
chuyển Expired và release chỉ Hold chưa release của những Booking vừa đổi. Chạy lại idempotent;
lỗi rollback toàn bộ, không đổi Confirmed/Cancelled hoặc gia hạn expiry.

| Biến môi trường | Mặc định |
|---|---|
| BOOKING_EXPIRY_CLEANUP_ENABLED | true |
| BOOKING_EXPIRY_CLEANUP_INTERVAL | PT5S; phải dương và không quá 5 giây |

## Kiểm chứng và bàn giao

Đã kiểm chứng P3 thật, snapshot Trip–Booking, expiry đồng thời và rollback trên PostgreSQL 16.
Lượt local ngày 17/11/2026: **329 tests pass**, không failure/error/skip, bằng lệnh
`docker compose -p team11-huy-booking-test -f compose.test.yaml run --rm backend-test`;
sau đó `docker compose -p team11-huy-booking-test -f compose.test.yaml down`.
Tests Java và fixture cố định Booking giữ local; riêng SQL runtime bên dưới được bàn giao qua PR.
Số 329 là kết quả toàn backend trước khi đóng gói, không phải số tests CI. Xem Checks của PR #5
trên đúng commit mới nhất; CI chạy trên source đã publish, không chứa các tests Booking local.
Giữ sửa scope MVC test nền tảng để không nạp H2 vào slice thiếu service; assertion cũ giữ nguyên.

## Demo cho Hoàng và QA

SQL runtime dùng nguyên UUID Account/Trip/Stop/ghế của Phương, không tự chạy khi app khởi động.
Chỉ nạp một lần vào DB demo riêng, sạch, đã áp dụng V1–V4. Với client `psql`, đặt
`BOOKING_DEMO_DATABASE_URL` tới DB đó; từ gốc repo chạy **một lệnh, một transaction**:

```bash
psql -X -v ON_ERROR_STOP=1 -1 --dbname="$BOOKING_DEMO_DATABASE_URL" \
  -f src/backend/src/test/resources/fixtures/identity_accounts.sql \
  -f src/backend/src/test/resources/fixtures/trip_demo_runtime.sql \
  -f src/backend/src/test/resources/fixtures/booking_availability_runtime.sql
```

Lỗi SQL dừng và rollback toàn bộ. Không chạy tách ba file. Seed không idempotent; muốn reset,
tạo lại đúng DB demo qua Flyway rồi nạp lại, không xóa volume phát triển hoặc dữ liệu dùng chung.
Trip dùng ngày mai theo giờ Việt Nam; Booking dùng một `seedTime` của transaction.
Hold B2 tạo trước 8 phút, hết hạn sau 2 phút; refresh không gia hạn. Job có thể dọn Hold đã cũ ngay.

| Ghế | Dữ liệu | A–C | B–C |
|---|---|---|---|
| A1 | Không allocation | FREE | FREE |
| A2 | Confirmed A–B | UNAVAILABLE | FREE |
| B1 | Confirmed A–C | UNAVAILABLE | UNAVAILABLE |
| B2 | Hold B–C, trước expiry | UNAVAILABLE | UNAVAILABLE |
| C1 | Hold đã hết hạn | FREE | FREE |
| C2 | Cancelled/released | FREE | FREE |

Sau khi leader mở public GET, đặt `BOOKING_API_BASE_URL` theo địa chỉ demo rồi thử A–C:

```bash
curl -i --get "$BOOKING_API_BASE_URL/api/v1/trips/11111111-1111-4111-8111-111111111111/seat-availability" \
  --data-urlencode 'fromStopId=22222222-2222-4222-8222-222222222222' \
  --data-urlencode 'toStopId=99999999-9999-4999-8999-999999999999'
```

Đổi fromStopId sang `33333333-3333-4333-8333-333333333333` để thử B–C. Hai biến URL trên chỉ
dành cho lệnh client, không thêm cấu hình backend. Confirmed có expiry null; chỉ Hold chặn có expiry.
Sau mốc expiry B2 phải FREE dù cleanup trễ. Lỗi truy xuất phải là 503, không có bản đồ FREE giả.

## Bàn giao PA4

- H1/V4, H2, H3 đã có trên PR #5; P3 head `122d965` đã merge qua `5891546`. Đã kiểm chứng đủ fare
  toàn chuyến và cùng snapshot Trip–Booking với một asOf; không cần chờ P2/P4 để ghép H2.
- Nội dung SAD/weekly phần Huy: GET chỉ đọc qua port/core, H3 ghi transaction riêng; ba bảng V4,
  FK và luồng lỗi 503 mô tả ở trên. Không có API tạo Booking hoặc giữ ghế trong lần xem.
- Minh Phương cần review phần mới của PR #5; nhận xét core cũ chưa thay review V4/H2/H3.
- Leader C2 sở hữu public GET/security chung; Hoàng nối F4; Đức Hiếu tổng hợp SAD/weekly và QA.
  Chưa thực thi năm ca public: map A–C, B–C kề nhau, expiry/cleanup, hết ghế vẫn 200, lỗi đọc 503.
  Các ca hết ghế/lỗi hạ tầng chỉ dựng trên DB demo riêng; kết quả local không thay báo cáo QA.

Contract: `docs/contracts/openapi.yaml`, `docs/contracts/trip-query-port.md`, C1 và UC-P07/BR-P07-01–04.
Chưa có P08/P09, Payment, Ticket hoặc frontend. PR/CI xanh chưa phải nghiệm thu đầy đủ UC-P07.
