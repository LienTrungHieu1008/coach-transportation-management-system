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
Tests/fixtures Booking giữ local theo yêu cầu, không đưa vào phần commit; số 329 là kết quả toàn
backend trước khi đóng gói, không phải kết quả CI của PR. Giữ duy nhất sửa scope MVC test nền tảng
để controller H2 không bị nạp vào slice thiếu application service; các assertion cũ giữ nguyên.

Contract: `docs/contracts/openapi.yaml`, `docs/contracts/trip-query-port.md`, C1 và UC-P07/BR-P07-01–04.
Chưa có P08/P09, Payment, Ticket hoặc frontend. Leader C2 còn cần mở đúng public GET, kiểm tra
security chung; QA chạy năm ca public P07 và Minh Phương/leader review trước nghiệm thu đầy đủ.
