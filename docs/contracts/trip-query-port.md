# TripQueryPort — đề xuất điểm ghép P07

Trạng thái: **review candidate 0.1**, cùng [OpenAPI](openapi.yaml) và [quyết định API](pa4-api-decisions.md). Đây là đặc tả, chưa có interface/class production. Phương sở hữu port tại `vn.edu.team11.coach.trip.contract`; Huy chỉ import contract, không entity/repository Trip. Tên `TripQueryPort` theo SAD, chưa mở rộng sang AI ngoài task.

## Giao diện cần thống nhất

Chữ ký Java dự kiến:

```java
TripSeatFacts getSeatAvailabilityFacts(
    UUID tripId, UUID fromStopId, UUID toStopId, Instant asOf);
```

`asOf` do application P07 lấy từ clock server/DB có thẩm quyền, không nhận từ query/client. Cùng một asOf dùng cho eligibility, kiểm tra hold và `serverTime` response. Caller không tự lấy lại thời gian khác cho từng ghế. Không dùng kết quả port để đảm bảo tạo hold trong tương lai; P08 phải kiểm tra lại trong transaction riêng khi được triển khai.

`TripSeatFacts` là immutable DTO gồm:

| Trường | Kiểu/ý nghĩa |
|---|---|
| tripId | UUID Trip đã xác minh |
| fromStopId, toStopId | UUID các Stop của Trip |
| fromStopIndex, toStopIndex | int thứ tự Trip, 0-based; `from < to`; Segment `[from,to)` |
| status | enum Trip `Scheduled/Departed/Completed/Cancelled` |
| bookingClosesAt | Instant, giờ rời Stop đầu trừ 30 phút |
| seatLayout | DTO tương đương OpenAPI `SeatLayout`; chứa UUID vật lý thật, label, deck/row/column; không entity/proxy |
| asOf | Instant chính caller cung cấp để xác nhận snapshot tính toán |

Port chỉ trả facts thành công nếu Trip Scheduled, `asOf < bookingClosesAt`, Segment hợp lệ và đủ essential data theo BR-P05-01. Không trả facts có layout rỗng hoặc Stop indices chưa xác minh. Không trả allocations hoặc Booking vì đó không thuộc ownership Trip.

## Kết quả lỗi có kiểu

Phương công bố `TripQueryException` cùng `TripQueryErrorCode` trong `trip.contract` để caller không bắt persistence exception của Trip:

| Code | Trường hợp | P07 ánh xạ HTTP |
|---|---|---|
| TRIP_NOT_FOUND | UUID đúng format nhưng không tồn tại Trip | 404 |
| STOP_NOT_SERVED | Stop không tồn tại/không được phục vụ | 422 |
| INVALID_SEGMENT | Stop không thuộc Trip, bằng nhau hoặc đảo thứ tự | 422 |
| TRIP_NOT_BOOKABLE | Không Scheduled hoặc tại/sau cutoff | 409 |
| ESSENTIAL_DATA_INCOMPLETE | Dữ liệu cấu hình bắt buộc thiếu/không hợp lệ | 409 |
| TRIP_DATA_UNAVAILABLE | Không thể đọc/xác minh facts vì lỗi hạ tầng | 503 `AVAILABILITY_UNAVAILABLE` khi qua API P07 |

P07 validate format trước khi gọi port (400). Nếu nhiều lỗi cùng tồn tại, thứ tự đề xuất: format → Trip tồn tại → đủ dữ liệu để xác minh Segment → Segment → eligibility. Thiếu cấu hình khiến không xác minh được Segment là ESSENTIAL_DATA_INCOMPLETE, không đoán là INVALID_SEGMENT. Không đính kèm SQL/stack hoặc dữ liệu nhạy cảm vào HTTP response.

## Transaction và consistency

- Application P07 điều phối trong transaction read-only REQUIRED cùng DataSource/transaction manager; dùng snapshot nhất quán cho Trip/layout/allocations (đề xuất PostgreSQL REPEATABLE READ). Trip implementation tham gia transaction caller, không REQUIRES_NEW và không gọi provider.
- Không khóa ghế khi xem và không ghi Booking. Availability là snapshot informational; không cam kết còn ghế khi request sau bắt đầu.
- Huy truy vấn allocations thuộc Booking và kiểm tra điều kiện hiệu lực, overlap trên indices đã được port xác minh. Không truy vấn trực tiếp bảng Trip để tìm layout/thứ tự.
- Job cleanup thuộc Booking thực hiện transaction ghi riêng, idempotent; không được phụ thuộc việc người dùng mở trang. Nếu job trễ, query vẫn loại hold có `asOf >= expiresAt`.

## Các test contract hai phía cần có khi triển khai

- Phương: Stop order/Trip existence/layout thật; Scheduled ở đúng cutoff bị từ chối; missing essential vs lỗi truy xuất; facts/asOf/IDs đúng input đã xác minh.
- Huy: A→B và B→C không giao nhau, A→C và B→C giao nhau; Confirmed/hold hiệu lực; hold tại expiry không chặn; cancelled/released không chặn; một-to-một ID ghế; lỗi port được map đúng và không giả FREE.
- Tích hợp thật: port tham gia snapshot cùng transaction, chạy với PostgreSQL test riêng. DTO không lộ entity/lazy proxy; không chỉ mock port rồi coi persistence consistency đã kiểm chứng.
