# API PA4 — bản đề xuất để chốt cùng leader

Ngày 12/11/2026. **Người giao việc đã đồng ý cấu trúc bốn GET công khai.** Bản đề xuất này được giữ làm bối cảnh; chi tiết hiện tại nằm trong [OpenAPI 0.1](openapi.yaml), [quyết định và trạng thái review](pa4-api-decisions.md), [TripQueryPort](trip-query-port.md). DTO và các lựa chọn bổ sung là review candidate, chưa có implementation hoặc approval của từng chủ module. Không đổi security hoặc tạo endpoint trong bước contract.

Nguồn: [UC 1.8](../requirements/Team%2011%20-%20rup_ucspec-v2_final.pdf), P04 trang 10–12, P05 trang 12–13, P07 trang 15–16; [SAD 1.3](../analysis%20and%20design/PA3_Group11_SAD_v1.3_final.pdf), phần API/DTO, ownership và contract Segment. Các lựa chọn HTTP/DTO bổ sung dưới đây là đề xuất hiện thực, không phải mọi chi tiết đã được PA3 duyệt.

## Cách tổ chức đề xuất

Giữ REST `/api/v1` như endpoint seat-availability đã có trong SAD. FE gọi ba API UC và một API danh mục Stop; backend chịu trách nhiệm validation, giá, cutoff và availability. Cách gom cả ba UC vào một API lớn giảm số request nhưng trộn ownership Trip/Booking và khó refresh ghế độc lập. GraphQL hoặc realtime push chưa cần cho phạm vi này; không thêm stack/dependency.

| Method/path | Chủ trì | Mục đích và đầu vào |
|---|---|---|
| `GET /api/v1/stops` | Phương | Danh mục Stop để chọn điểm lên/xuống, không hardcode trên FE. Đề xuất trả toàn bộ Stop đang được phục vụ, sort name rồi id; chưa paging vì danh mục demo nhỏ. |
| `GET /api/v1/trips` | Phương | P04: `fromStopId`, `toStopId`, `boardingDate=YYYY-MM-DD` bắt buộc; `page` mặc định 0, `size` mặc định 20, giới hạn 1–100. |
| `GET /api/v1/trips/{tripId}` | Phương | P05: nhận cặp `fromStopId`, `toStopId` nếu đi từ kết quả tìm. Khi không có cả hai, trả thông tin chuyến với `selectedSegment: null`, cho người xem chọn đoạn; không tự tính một giá đoạn mặc định. Chỉ có một tham số trong cặp là lỗi. |
| `GET /api/v1/trips/{tripId}/seat-availability` | Huy | P07: `fromStopId`, `toStopId` bắt buộc. Phương cung cấp Trip/seat layout/Segment qua port, Huy tính availability từ allocations thuộc Booking. Path và tên tham số giữ đúng SAD. |

Tất cả là GET công khai, không cần login/CSRF, không tạo Booking/hold. Allowlist chỉ đúng method/path đã triển khai; các route khác tiếp tục được bảo vệ. Không công bố account, contact, bookingId hoặc thông tin hành khách qua API ghế.

## Quy ước chung đề xuất

- ID là UUID string, JSON camelCase; enum Trip giữ `Scheduled/Departed/Completed/Cancelled`, enum availability giữ `FREE/UNAVAILABLE/UNKNOWN` theo SAD. Không dùng enum Booking để làm enum ghế.
- Thời điểm RFC3339 UTC có `Z`; response có `serverTime`, FE hiển thị theo `Asia/Ho_Chi_Minh`. `boardingDate` là ngày tại Stop lên, kể cả qua đêm.
- Tiền VND integer không âm; `fareAmountVnd` do BE cộng leg fares. Không dùng 0 thay cho giá thiếu; dùng null ở response chi tiết khi chưa xác minh được giá. Response tìm kiếm chỉ chứa fare đã xác minh.
- Không khớp kết quả là 200 với `items: []`; không phải 404. Sort tìm chuyến theo `boardingTime` tăng dần, tie-break bằng `tripId`. Paging `items/page/size/totalElements/totalPages`, tổng tính trên cùng tiêu chí lọc.
- Missing/null khác nhau: thiếu input bắt buộc là lỗi; trường output đã khai báo luôn hiện diện, null chỉ khi schema cho phép. Stop/Trip/seat IDs và thứ tự phải được BE kiểm tra.
- Chi tiết và availability dùng `Cache-Control: no-store`; FE không trình bày dữ liệu cũ như kết quả mới sau lỗi refresh. Bản đồ có thể giữ hình học đã tải nhưng trạng thái ghế phải chuyển UNKNOWN khi không xác minh được.
- Error body đề xuất khớp hình dạng hiện có: `{code,message,fieldErrors:[{field,message}]}`. `traceId` chưa bắt buộc vì baseline chưa có tracing. Message để hiển thị, FE rẽ nhánh theo code/status. Không trả stack/SQL.

## P04 — tìm chuyến

Item tối thiểu: `tripId`, `route:{id,name}`, `fromStop:{id,name}`, `toStop:{id,name}`, `boardingTime`, `alightingTime`, `vehicleType`, `fareAmountVnd`, `bookingClosesAt`. Envelope thêm `serverTime` và các trường paging.

BR-P04: Stops được phục vụ và tồn tại thứ tự lên trước xuống; ngày không quá khứ theo Việt Nam; chỉ Scheduled còn mở bán. Cutoff dùng giờ rời Stop đầu tuyến trừ 30 phút cho mọi Segment, không dùng giờ Stop lên. Đề xuất tại đúng cutoff: `now >= bookingClosesAt` là đóng, cần chủ UC xác nhận khi duyệt contract. Không suy ra số ghế trống hoặc đảm bảo giữ ghế từ kết quả search.

Nếu cặp Stop hợp lệ nhưng không có chuyến/ngày phù hợp, trả rỗng. Nếu không có Route phục vụ cặp Stop theo đúng thứ tự, đề xuất 422 `INVALID_SEGMENT`. Record không đủ dữ liệu giá/thời gian không được tạo giá/giờ giả; cách loại record lỗi khỏi kết quả hay báo lỗi toàn truy vấn cần chủ Trip xác nhận trước khi duyệt OpenAPI.

## P05 — xem chi tiết

Response đề xuất: `tripId`, `status`, `route`, `vehicle` (nullable nếu dữ liệu thiếu), `stops` có thứ tự cùng giờ đến/rời (nullable khi thiếu), `legFares`, `description` nullable, `selectedSegment` nullable, `bookingClosesAt` nullable, `bookingEligibility:{allowed,reasons:[]}`, `serverTime`.

Khi có Segment hợp lệ, `selectedSegment` gồm `fromStopId/toStopId/boardingTime/alightingTime/fareAmountVnd`; field giá/giờ có thể null nếu dữ liệu thiết yếu thiếu, đồng thời eligibility phải false. `reasons` có thể gồm `TRIP_NOT_SCHEDULED`, `BOOKING_CLOSED`, `ESSENTIAL_DATA_INCOMPLETE`; danh sách trả đủ lý do xác định được. `allowed` chỉ phản ánh điều kiện Trip/dữ liệu, không đảm bảo còn ghế. Khi chưa chọn Segment, FE yêu cầu chọn trước khi mở P07.

Trip đóng bán hoặc thiếu dữ liệu thiết yếu vẫn 200 với thông tin xác minh được và eligibility false theo P05 A2/A3. Thiếu mô tả tùy chọn không chặn; không tự điền mô tả/xe/giá giả. Trip không tồn tại 404. Không tải được records vì lỗi hệ thống là 503, khác với records có thật nhưng thiếu cấu hình.

## P07 — xem ghế

Response đề xuất: `tripId`, `fromStopId`, `toStopId`, `serverTime`, `bookingClosesAt`, `layout:{id,seats:[{physicalSeatId,label,deck,row,column}]}`, `seats:[{physicalSeatId,status,expiresAt}]`.

- Hình học layout thuộc Trip; Huy nhận DTO/port của Phương, không đọc repository Trip. Kiểu tọa độ và quy ước deck bắt đầu từ đâu phải đối chiếu layout SAD và chốt trước OpenAPI.
- Trả đầy đủ ghế vật lý thuộc layout, mỗi ghế đúng một trạng thái. Không có ghế FREE vẫn 200 để UI hiển thị hết ghế.
- Confirmed và Pending/Hold chưa released với `serverTime < expiresAt` chặn Segment giao nhau; hết hạn, cancelled hoặc released không chặn. Hai đoạn `[a,b)` và `[b,c)` không giao nhau. Không chỉ so trùng Stop hoặc chỉ kiểm tra cùng Trip.
- `expiresAt` nullable, chỉ dùng cho blocker hold hiện hành; không lộ Booking. Nếu nhiều blocker trên các đoạn con giao nhau, đề xuất trả mốc hết hạn sớm nhất để yêu cầu refresh, không hứa ghế sẽ FREE ở mốc đó. Nếu còn blocker Confirmed thì trạng thái vẫn UNAVAILABLE, expiresAt có thể null.
- FE dùng chênh lệch `expiresAt - serverTime` để hẹn refresh; đề xuất polling 15 giây khi tab hiển thị, refresh khi quay lại tab và khi đến expiry/cutoff. Không tự đổi ghế thành FREE dựa vào đồng hồ client; không gia hạn hold khi refresh.
- Khi không thể tải/kiểm chứng allocations hoặc layout, trả 503 `AVAILABILITY_UNAVAILABLE`; FE hiển thị unknown và cho retry. Không giả FREE. UNKNOWN trong 200 chỉ được dùng nếu backend xác định được phần chưa biết một cách an toàn, không dùng để che lỗi toàn truy vấn.
- P07 A4 yêu cầu Pending chuyển Expired. Đề xuất job trong Booking thực hiện cleanup idempotent theo server clock; GET không có side effect nghiệp vụ. Mọi query vẫn loại hold hết hạn ngay cả khi job chạy trễ, theo SAD D6. Tần suất cleanup và tiêu chí kiểm thử chuyển trạng thái phải chốt; không coi chỉ loại hold khỏi map là đã thực hiện đủ A4.

## Status/lỗi đề xuất

| Status | Khi nào | Code dự kiến |
|---|---|---|
| 400 | Thiếu tham số, sai UUID/date format, paging ngoài giới hạn, thiếu một nửa cặp Stop | `INVALID_REQUEST` |
| 422 | Date quá khứ, Stop không được phục vụ, thứ tự/Segment không hợp lệ | `PAST_BOARDING_DATE`, `INVALID_SEGMENT`, `STOP_NOT_SERVED` |
| 404 | Không có Trip cho P05/P07 | `TRIP_NOT_FOUND` |
| 409 | P07 trên Trip đóng bán/không Scheduled hoặc chưa đủ dữ liệu thiết yếu | `TRIP_NOT_BOOKABLE`, `ESSENTIAL_DATA_INCOMPLETE` |
| 503 | Không thể truy xuất dữ liệu Trip/availability | `TRIP_DATA_UNAVAILABLE`, `AVAILABILITY_UNAVAILABLE` |
| 500 | Lỗi nội bộ không được phân loại an toàn | `INTERNAL_ERROR` |

SAD mapping UI dùng 422 cho input nghiệp vụ không hợp lệ; vì vậy đề xuất phân biệt 400 cấu trúc với 422 nghiệp vụ thay vì đổi tất cả về 400. 401/403 của baseline vẫn áp dụng cho route bảo vệ, không phải yêu cầu login cho ba UC công khai này.

## Cần chốt trước khi giao code

1. Đã đồng ý cấu trúc bốn GET riêng; không suy ra tất cả DTO/status/chi tiết đều đã được duyệt.
2. Đã cụ thể hóa OpenAPI/schema/layout/port và cách xử lý dữ liệu thiếu để review, xem bảng lựa chọn trong `pa4-api-decisions.md`.
3. Phương/Huy/Hoàng và QA review các lựa chọn, leader chốt phiên bản contract trước khi code; migration được phân số chung, không sửa V1 session.
4. Task contract không tự cấp quyền đổi schema DB/security hoặc triển khai nghiệp vụ.
