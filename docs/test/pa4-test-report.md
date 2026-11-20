# PA4 — Báo cáo kiểm thử (Test Report) Nhóm 11

Task Q3 · Ngày thực thi: **20/11/2026** (lượt thử trước ngày 19/11 bị gián đoạn do tắt máy, không tính); retest bản sửa `5bd859b` và `6ad7f73` (PR #20) cùng ngày ·
Người thực thi: Đinh Đức Hiếu (thực thi cùng Claude Code trên máy của Đức Hiếu) · Người duyệt: Liên Trung Hiếu (chưa duyệt).

Bản Excel theo mẫu Moodle: [PA4-Group11-test-report.xlsx](PA4-Group11-test-report.xlsx) (Test summary + Bug report).
Kết quả từng ca đã điền vào [PA4-Group11-test-cases.xlsx](PA4-Group11-test-cases.xlsx) (cột Actual Result, Status, Tester, Tested Date, Remark).
Bằng chứng thô: [evidence/pa4-q3/](evidence/pa4-q3/) — JSON từng request, ảnh giao diện, nhật ký chạy, tóm tắt backend verify.

## 1. Tóm tắt

| UC | Số ca | Pass (API/UI) | Pass (test tự động) | Pass một phần | Fail | Chưa chạy |
|---|---:|---:|---:|---:|---:|---:|
| UC-P04 Search Trips | 10 | 9 | 1 | 0 | 0 | 0 |
| UC-P05 View Trip Details | 10 | 6 | 2 | 2 | 0 | 0 |
| UC-P07 View Seat Availability | 10 | 7 | 0 | 1 | 2 | 0 |
| **Tổng** | **30** | **22** | **3** | **3** | **2** | **0** |

**Mức kiểm chứng:**
- **27/30 ca** chạy qua API và/hoặc giao diện trên stack `pa4-v1.0` với backend + PostgreSQL thật. Mỗi UC có ≥ 5 ca loại này (P04: 9, P05: 8, P07: 10).
- **3 ca `TC-P04-07`, `TC-P05-06`, `TC-P05-10`** chỉ có bằng chứng test tự động trong lượt backend verify chạy lại trên cùng commit. Các ca này **chưa chạy qua API/UI trong lượt demo** và được thống kê riêng ở cột "Pass (test tự động)".
- Một số ca khác dùng test tự động cho một bước con, còn các bước chính chạy qua API/UI. Đó là phần quá cutoff của `TC-P04-08`, `TC-P07-06` và phần lỗi truy xuất của `TC-P04-10`, `TC-P07-08`; cột "Mức" ở mục 4 ghi rõ.
- Luồng chính P04 → P05 → P07 chạy đúng trên giao diện: giá theo chặng, giờ, cutoff, trạng thái ghế theo Segment, Hold hết hạn và job dọn hold đều khớp expected.
- **3 lỗi mức Low**, đều ở giao diện, giao **Khánh Hoàng**; không có lỗi Critical/High/Medium. Không phát hiện lỗi ở phần tìm chuyến/giá (Phương) hay trạng thái ghế/hold (Huy).
- "Pass một phần": các điểm ER chạy được đều đạt, nhưng một bước UI không thực thi được (lý do ở mục 4). Không ghi Pass cho bước chưa chạy.
- Bảng trên là kết quả trên `pa4-v1.0` và được giữ nguyên.
- **Retest ngày 20/11/2026** trên bản sửa ([PR #20](https://github.com/LienTrungHieu1008/coach-transportation-management-system/pull/20), **chưa merge**, commit mới nhất `6ad7f73`): cả 4 ca liên quan B01–B03 đều đạt. Chi tiết ở mục 5.1.

## 2. Bản build và môi trường

| Hạng mục | Giá trị |
|---|---|
| Bản kiểm thử | tag `pa4-v1.0` → commit `6e4f9aa11829e7e57f989618b9002281303798f4` (trùng `main` ngày 20/11) |
| Cách dựng | `git worktree` tách riêng tại tag; `docker compose -p team11-qa -f compose.yaml -f compose.qa.yaml up --build -d --wait`. Override `compose.qa.yaml` chỉ đổi DB sang tmpfs và tên DB/user riêng; không đụng volume phát triển. Image `team11-qa-backend` `dea23a416666`, `team11-qa-web` `27f3515ade3f`. |
| Thành phần | web `nginx:stable-alpine` (127.0.0.1:8088) → backend Spring Boot 3.5 / `eclipse-temurin:21-jre` → `postgres:16` (16.13), Flyway V1–V4. Job dọn hold bật mặc định, chu kỳ PT5S. |
| Máy | Windows 11 Home 10.0.26300, Docker Desktop 4.70 (Engine 29.4.0, Compose v5.1.2) |
| Trình duyệt | Trình duyệt tích hợp của Claude desktop (Chromium/Chrome 156) — desktop 1024×768; giả lập mobile 375×812 (UA Android Chrome 156) cho TC-P07-04. Chưa thử Edge/Safari. |
| Gọi API | Python `urllib` (script ngoài repo) gọi qua `http://127.0.0.1:8088/api/v1/...`; lưu nguyên URL, status, `Cache-Control`, body. |
| Backend verify | `docker compose -p team11-qa-verify -f compose.test.yaml run --rm backend-test` trên cùng commit: **587 tests, 0 failure/error/skip** (299 unit + 288 IT, PostgreSQL 16). Dùng cho các ca mức IT. |

## 3. Dữ liệu kiểm thử

Mọi dữ liệu nạp vào DB tmpfs riêng của project `team11-qa`, từ fixture trong repo, theo
[Booking README](../../src/backend/src/main/java/vn/edu/team11/coach/booking/README.md) và
[kịch bản QA P1–P4](p1-p4-runtime-qa-scenarios.md). Seed lúc 02:24:57Z ngày 20/11 nên **D = 21/11/2026**
(Trip ngày: A 09:00 → B 11:00 → C 13:00, đóng bán 08:30; Trip qua đêm: A 22:30 → B 00:30 (22/11) → C 02:30, đóng bán 22:00).

| Vòng | Dữ liệu | Ca |
|---|---|---|
| A | `identity_accounts.sql` → `trip_demo_runtime.sql` → `booking_availability_runtime.sql` (Hold B2 hết hạn 02:26:58Z) | P04-01…06, 09; P05-01…04, 08, 09; P07-01, 02, 03, 05, 08 (UI), 09, 10 |
| B | DB mới, chỉ Account + Trip; áp/khôi phục lần lượt `status-apply/restore` (Cancelled, Departed, Completed), `fare-bc-*`, `seat-c2-*`; chạy `_guard.sql` sau mỗi lần khôi phục (đều exit 0) | P04-08, P04-10 (runtime), P05-05, P05-07, P07-06, P07-07 |
| C | DB mới, Account + Trip + `booking_all_unavailable_runtime.sql` | P07-04 |

## 4. Kết quả từng ca

Cột "Mức": API = gọi HTTP; UI = thao tác trên giao diện; DB = truy vấn đọc; IT = đọc kết quả test tự động trong lượt verify ở mục 2.

### UC-P04 Search Trips

| Ca | Mức | Kết quả thực tế | Status |
|---|---|---|---|
| TC-P04-01 | UI+API | 200, 2 chuyến theo giờ lên: Trip ngày 09:00→11:00 và qua đêm 22:30→00:30 (22/11), tuyến "Demo Route A-B-C" (mã DEMO-ABC), "Demo coach", 100.000đ, đóng bán 08:30 / 22:00. Danh mục Stop: A, B, C. | Pass |
| TC-P04-02 | UI+API | 2 chuyến, `fareAmountVnd = 250000` do server trả; giờ là giờ tại A và C (09:00→13:00, 22:30→02:30). | Pass |
| TC-P04-03 | UI+API | B→C ngày 22/11: chỉ Trip qua đêm, lên B 00:30 22/11, 150.000đ, đóng bán 22:00 21/11. B→C ngày 21/11: chỉ Trip ngày (lên B 11:00). | Pass |
| TC-P04-04 | UI+API | UI chặn ngày 19/11 bằng ràng buộc của ô ngày ("Value must be 11/20/2026 or later", thông báo của trình duyệt), giữ tiêu chí, không gửi tìm kiếm. API: 422 `PAST_BOARDING_DATE`. | Pass |
| TC-P04-05 | UI+API | API: A→A và C→A 422 `INVALID_SEGMENT`; Stop UUID lạ 422 `STOP_NOT_SERVED`. UI: A→A bị chặn tại form ("Điểm đón và điểm trả phải khác nhau"); C→A hiện lỗi, không hiện kết quả rỗng, có "Quay lại" để sửa. Stop lạ không chọn được trên UI (danh mục chỉ có Stop phục vụ). Ghi lỗi **B01** về cách hiển thị. | Pass (kèm B01) |
| TC-P04-06 | UI+API | 200, `items=[]`, `totalElements=0`; UI "Không có chuyến phù hợp — Hãy thử ngày hoặc điểm lên, xuống khác." | Pass |
| TC-P04-07 | IT | **Chưa chạy qua API/UI trong lượt demo** (cutoff thật là 08:30 21/11, không có cơ chế đổi clock lúc chạy). Bằng chứng test tự động PASS: `TripSearchPolicyTest.exactCutoffIsClosedAndInstantBeforeIsOpen`, `PostgresTripSearchRepositoryIT.cutoffAndCompleteFareCoverageAgreeWithExistingP3Rules`, `overnightIntermediateBoardingStillClosesAtFirstStopCutoff`. | Pass (test tự động) |
| TC-P04-08 | API+IT | Trip ngày Cancelled / Departed / Completed: tìm A→C ngày D chỉ còn Trip qua đêm (1/1). "Scheduled đã qua cutoff" theo IT `TripSearchPolicyTest.knownWrongDateOrClosedTripDoesNotFailForMissingFares` và các test cutoff ở TC-P04-07. | Pass |
| TC-P04-09 | API | Thiếu `boardingDate`, `fromStopId=abc`, `size=101`: cả ba 400 `INVALID_REQUEST`, `fieldErrors` đúng một trường sai; message không có SQL/stack trace. | Pass |
| TC-P04-10 | IT+API+UI | IT PASS: `TripSearchControllerTest.retrievalFailureIs503AndDoesNotExposeSql`, `PublicTripsSecurityIT.anonymousSearchRetainsValidationAndDatabaseFailureMapping`, `TripSearchServiceTest.emptyResultHasZeroPagesAndRetrievalFailuresAreNotReplacedByEmptySuccess`. Runtime: thiếu LegFare B–C hoặc ghế C2 → 503 `TRIP_DATA_UNAVAILABLE`, không trả danh sách một phần. UI: "Không thể tải dữ liệu" + "Thử lại", không hiện danh sách; sau khi khôi phục dữ liệu, "Thử lại" tải được 2 chuyến. | Pass (kèm B01) |

### UC-P05 View Trip Details

| Ca | Mức | Kết quả thực tế | Status |
|---|---|---|---|
| TC-P05-01 | UI+API | 200 `Scheduled`; Stop 0 A rời 09:00, 1 B 11:00, 2 C đến 13:00; `legFares` đúng 2 dòng (A–B 100.000, B–C 150.000); `selectedSegment.fareAmountVnd=250000`; đóng bán 08:30; `allowed=true`, `reasons=[]`; nút "Xem tình trạng ghế" bật. | Pass |
| TC-P05-02 | API | API: 200, `selectedSegment=null`, vẫn có Stop/giờ/legFares, `allowed=true`. Bước 2 (UI khi chưa chọn Segment) **không thực thi được**: giao diện chỉ mở chi tiết từ kết quả tìm, luôn kèm Segment. | Pass một phần |
| TC-P05-03 | API | 400 `INVALID_REQUEST`, `fieldErrors[toStopId]` "Stop parameters must be supplied together". | Pass |
| TC-P05-04 | API | API: 404 `TRIP_NOT_FOUND`. Phần UI (mở URL chi tiết với tripId lạ) **không thực thi được**: SPA không có URL riêng cho trang chi tiết. | Pass một phần |
| TC-P05-05 | API+UI | Cancelled/Departed/Completed: 200, giữ đúng status, Stop/giờ/giá; `allowed=false`, `reasons=[TRIP_NOT_SCHEDULED]`. UI (Cancelled): nhãn "Không thể tiếp tục", "Chuyến không còn mở bán.", nút xem ghế `disabled`. | Pass |
| TC-P05-06 | IT | **Chưa chạy qua API/UI trong lượt demo** (lý do như TC-P04-07). Bằng chứng test tự động PASS: `TripDetailsPolicyTest.cutoffEqualityIsClosed` (3 biến thể), `PostgresTripDetailsRepositoryIT.q1CutoffUsesFirstStopForEverySegmentAndMatchesP3`, `q1MultipleReasonsAccumulateEvenWhenTripIsClosed`. | Pass (test tự động) |
| TC-P05-07 | API+UI | Thiếu LegFare B–C: A→C 200, `allowed=false` `[ESSENTIAL_DATA_INCOMPLETE]`, `legFares` chỉ A–B, `fareAmountVnd=null` (không 0); A→B vẫn giữ 100.000. Thiếu ghế C2: 200, `[ESSENTIAL_DATA_INCOMPLETE]`. UI (A→B): chỉ liệt kê giá A→B, báo "Thông tin thiết yếu chưa đủ…", nút ghế `disabled`. | Pass |
| TC-P05-08 | UI+API | 200, `description=null`; UI "Mô tả tùy chọn chưa được cung cấp."; `allowed=true`; vào màn ghế được. | Pass |
| TC-P05-09 | API | B→A: 422 `INVALID_SEGMENT`, không có `selectedSegment`. | Pass |
| TC-P05-10 | IT | **Chưa chạy qua API/UI trong lượt demo** (không có cách gây lỗi truy xuất DB cho riêng API chi tiết mà không sửa source). Bằng chứng test tự động PASS: `TripDetailsControllerTest.databaseAndTransactionFailuresAre503WithoutPrivateCause`, `PublicTripDetailsSecurityIT.anonymousActualSqlFailureIs503AndDoesNotLeakInternals`, `TripDetailsServiceTest.retrievalErrorIsNeverConvertedToNotFound`. | Pass (test tự động) |

### UC-P07 View Seat Availability

Ghế theo manifest: A1 (ghế 1), A2 (2, Confirmed A–B), B1 (3, Confirmed A–C), B2 (4, Hold B–C), C1 (5, Hold đã hết hạn), C2 (6, Cancelled/released).

| Ca | Mức | Kết quả thực tế | Status |
|---|---|---|---|
| TC-P07-01 | UI+API | A→B: 200, layout `bbbbbbbb-…` đủ 6 ghế đúng UUID/nhãn/toạ độ, `seats` cùng thứ tự; A2, B1 UNAVAILABLE; A1, B2, C1, C2 FREE với `expiresAt=null`; đóng bán 08:30. UI "4 ghế còn trống". | Pass |
| TC-P07-02 | API | B→C: A2 FREE; B1 UNAVAILABLE expiry null; B2 UNAVAILABLE `expiresAt=02:26:58.184413Z` (đúng hạn hold trong DB); C1, C2, A1 FREE. | Pass |
| TC-P07-03 | UI+API+DB | Trước hạn: B2 "Không khả dụng — Giữ đến 09:26:58". UI tự gọi lại đúng lúc hết hạn (02:26:58.180) và B2 thành "Còn trống" mà không tải lại trang. DB: Booking `…dd03` chuyển Expired, Hold `released_at=02:26:58.280Z` do job; Confirmed không đổi. | Pass |
| TC-P07-04 | API+UI | API: A→B, A→C, B→C đều 200, 6 ghế UNAVAILABLE, expiry null. UI: 6 ghế "Không khả dụng", "0 ghế còn trống", không báo lỗi, không ghế FREE; không tràn ngang ở 375 px. **Thiếu gợi ý chọn chuyến/đoạn khác** → **B03**. | Fail |
| TC-P07-05 | API | B→A 422 `INVALID_SEGMENT`; tripId lạ 404 `TRIP_NOT_FOUND`; thiếu `fromStopId` 400 `INVALID_REQUEST`; không trả sơ đồ ghế. | Pass |
| TC-P07-06 | API+IT+UI | API: Cancelled/Departed/Completed → 409 `TRIP_NOT_BOOKABLE`. Quá cutoff: IT `SeatAvailabilityIT.firstStopCutoffAllowsOneMicrosecondBeforeAndRejectsExactOrAfterEvenForBc`, `PostgresTripQueryAdapterIT.tripAtExactCutoffReturnsNotBookable`. UI (đang xem ghế thì Trip bị huỷ): báo "Chuyến xe đã đóng bán", có "Về chi tiết chuyến", mọi ghế "Chưa xác minh" — nhưng **vẫn hiện lưới sơ đồ ghế** dưới tiêu đề "Không thể tải dữ liệu" kèm "Thử lại" → **B02**. | Fail |
| TC-P07-07 | API | Thiếu ghế C2 và thiếu LegFare B–C: 409 `ESSENTIAL_DATA_INCOMPLETE`, không sinh ghế. (V3 không cho xoá layout của xe đang gán; thiếu một PhysicalSeat là trường hợp hợp lệ gần nhất.) | Pass |
| TC-P07-08 | IT+UI | IT PASS: `SeatAvailabilityIT.actualDatabaseReadFailureIsUnavailableAndNeverFalseFree` (2 biến thể), `SeatAvailabilityApplicationServiceTests.failedFactReadIsUnavailableWithoutFreeFallback`, `failedAllocationReadIsUnavailableWithoutFreeFallback`, `SeatAvailabilityControllerTests.failedReadIsUnavailable`. UI: dừng container backend khi đang xem ghế → "Không thể tải dữ liệu" + "Thử lại", cả 6 ghế "Chưa xác minh", không ghế nào FREE. | Pass |
| TC-P07-09 | UI | Poll đúng mỗi 15 s khi tab hiển thị (12 request 02:25:27→02:28:27), thêm 1 request đúng lúc hết hạn hold; ẩn tab ~34 s không có request; quay lại tab gọi ngay (02:29:01). Bước 3 (response cũ về muộn) **không tái hiện được ở runtime** khi không sửa source. | Pass một phần |
| TC-P07-10 | UI+DB+API | Đếm `booking/booking_seat/seat_allocation` = 5/5/5 trước và sau khi xem ghế, refresh nhiều lần, qua lại chi tiết ↔ ghế; không có Pending mới. Mọi response trong lượt chạy có `Cache-Control: no-store`. Segment A→C được giữ khi quay lại chi tiết rồi vào lại ghế. | Pass |

## 5. Danh sách lỗi

| ID | Tiêu đề | Ca | Mức | Owner | Trạng thái |
|---|---|---|---|---|---|
| B01 | Lỗi nghiệp vụ/503 của tìm chuyến hiện message tiếng Anh của server, bị lặp và đặt dưới "Không thể tải dữ liệu" + "Thử lại" | TC-P04-05, TC-P04-10 | Low | Khánh Hoàng (UI); phối hợp Minh Phương (message backend) | Đã sửa trên PR #20, retest Pass; chờ review/merge |
| B02 | Màn ghế vẫn hiện lưới sơ đồ (toàn "Chưa xác minh") khi API trả 409 `TRIP_NOT_BOOKABLE` / `ESSENTIAL_DATA_INCOMPLETE` | TC-P07-06 | Low | Khánh Hoàng | Đã sửa trên PR #20, retest Pass; chờ review/merge |
| B03 | Hết ghế không có gợi ý chọn chuyến/đoạn khác | TC-P07-04 | Low | Khánh Hoàng | Đã sửa trên PR #20, retest Pass; chờ review/merge |

Trên `pa4-v1.0` cả ba lỗi vẫn tồn tại. Lỗi chỉ được đóng (Closed) khi PR #20 được merge vào `main`.

**B01 — tái hiện.** Bản `pa4-v1.0`, dữ liệu vòng A. Trang chủ chọn điểm đón Demo Stop C, điểm trả Demo Stop A, ngày 21/11/2026, bấm Tìm chuyến.
Mong đợi: thông báo tiếng Việt tương ứng mã `INVALID_SEGMENT`, cho sửa tiêu chí. Thực tế: tiêu đề "Không thể tải dữ liệu",
dòng "Boarding Stop must precede alighting Stop." hiện hai lần, nút "Thử lại" (thử lại không giúp gì với lỗi dữ liệu nhập).
Tương tự với 503: "Trip information could not be verified. Please retry." Module Booking trả message tiếng Việt, module Trip
trả tiếng Anh nên giao diện lẫn hai ngôn ngữ. Ảnh: `evidence/pa4-q3/ui/TC-P04-05_2_CA_error-english.jpg`, `TC-P04-10_search-503.jpg`.

**B02 — tái hiện.** Dữ liệu vòng B. Mở màn ghế Trip ngày A→C khi đang Scheduled; chạy `status-apply.sql` (Cancelled); chờ lần poll kế tiếp.
Mong đợi: không hiện sơ đồ ghế, nêu lý do, mời chọn chuyến/đoạn khác. Thực tế: tiêu đề "Không thể tải dữ liệu", lý do "Chuyến xe đã đóng bán",
nút "Thử lại", lưới 6 ghế vẫn hiển thị với trạng thái "Chưa xác minh". Không ghế nào hiện FREE nên không gây hiểu sai còn chỗ.
Tương tự với thiếu LegFare ("Chuyến xe thiếu dữ liệu bắt buộc"). Ảnh: `evidence/pa4-q3/ui/TC-P07-06_seats-after-cancel.jpg`.

**B03 — tái hiện.** Dữ liệu vòng C. Tìm A→C ngày 21/11 → Xem chi tiết Trip ngày → Xem tình trạng ghế.
Mong đợi: nêu rõ hết ghế và gợi ý chọn chuyến/đoạn khác. Thực tế: "0 ghế còn trống", 6 ghế "Không khả dụng", không có gợi ý.
Ảnh: `evidence/pa4-q3/ui/TC-P07-04_seats-AC-all-unavailable.jpg`.

### 5.1 Retest bản sửa

Bản sửa nằm trên nhánh `fix/f2-f4-error-states` ([PR #20](https://github.com/LienTrungHieu1008/coach-transportation-management-system/pull/20), chưa merge, chưa có review của Hoàng). PR chỉ đổi frontend; backend giống `pa4-v1.0`. Commit mới nhất được kiểm thử: **`6ad7f73`**. Có hai lượt retest trong cùng ngày 20/11/2026.

| Lượt | Commit | Thời gian (UTC) | Ca đã chạy | FE test (Node 24.19.0) |
|---|---|---|---|---|
| 1 | `5bd859b` | 03:28–03:32 | TC-P04-05, TC-P04-10, TC-P07-06, TC-P07-04 | 59 PASS |
| 2 | `6ad7f73` | 03:49–03:54 | TC-P07-06, TC-P07-08 | 61 PASS |

**Lượt 2:** `6ad7f73` sửa nốt B02. Ở `5bd859b`, sau khi nhận 409, mỗi lượt poll 15 s xoá `error` lúc bắt đầu gọi lại, nên sơ đồ ghế cũ hiện lại tạm thời. Commit này chỉ đổi `useSeatAvailability.ts` (giữ lỗi trong lúc refresh) và thêm test.
- Lượt 2 chạy lại TC-P07-06 và TC-P07-08 (nhánh lỗi tạm thời của cùng hook).
- **Không chạy lại** TC-P04-05, TC-P04-10, TC-P07-04 trên `6ad7f73`, vì commit không đụng tới màn tìm chuyến và nhánh hết ghế (không có lỗi). Kết quả của ba ca này lấy từ lượt 1; FE test của chúng vẫn PASS trên `6ad7f73`.

**Môi trường và dữ liệu:**
- Như mục 2: `compose.yaml` dựng từ worktree tại đúng commit, DB PostgreSQL 16 tmpfs riêng, trình duyệt tích hợp Chromium 156.
- Bước build image web chạy lại lint/typecheck/test/build (đạt).
- Dữ liệu: DB mới Account → Trip (D = 21/11/2026) với kịch bản `fare-bc` và `status` (Cancelled); lượt 1 thêm một DB mới với `booking_all_unavailable_runtime.sql`.
- Guard sau mỗi lần khôi phục đều exit 0.

| Ca | Lỗi | Lượt | Kết quả retest | Status |
|---|---|---|---|---|
| TC-P04-05 | B01 | 1 | C→A: tiêu đề "Đoạn đường chưa hợp lệ", thông điệp tiếng Việt, nút "Sửa tiêu chí tìm kiếm" (không còn "Thử lại", không còn câu tiếng Anh lặp). Quay về form vẫn giữ C, A, 21/11. | Pass |
| TC-P04-10 | B01 | 1 | Thiếu LegFare B–C: "Không thể tải dữ liệu — Chưa xác minh được thông tin chuyến xe. Hãy thử lại sau ít phút." kèm "Thử lại". Sau khi khôi phục dữ liệu, "Thử lại" tải được kết quả. | Pass |
| TC-P07-06 | B02 | 1 | Đang xem ghế A→B thì Trip chuyển Cancelled: "Chuyến không còn mở bán" với lý do và 2 nút "Về chi tiết chuyến", "Chọn chuyến khác"; không còn phần tử ghế nào (0). "Chọn chuyến khác" về danh sách đã tải lại (chỉ còn Trip qua đêm). | Pass |
| TC-P07-06 | B02 | 2 | Như lượt 1, thêm kiểm tra qua các lượt poll. Bị chặn lúc 03:50:41Z, sau đó poll thêm 3 lần (03:50:56, 03:51:11, 03:51:26); MutationObserver ghi nhận **0 lần** phần tử ghế xuất hiện lại. Khôi phục Scheduled thì lượt poll kế tiếp hiện lại 6 ghế, hết thông báo lỗi. | Pass |
| TC-P07-08 | — | 2 | Phần UI chạy lại vì hook thay đổi. Dừng backend → "Không thể tải dữ liệu" + "Thử lại", 6 ghế "Chưa xác minh", không ghế FREE. Bật lại backend → tự hồi phục (6 ghế, hết lỗi). | Pass |
| TC-P07-04 | B03 | 1 | Bộ hết ghế, A→C: khung "Đoạn này đã hết ghế trống" kèm nút "Chọn chuyến khác" và "Đổi điểm đón, điểm trả"; 6 ghế "Không khả dụng", không ghế FREE; không tràn ngang ở 375 px. | Pass |

Bằng chứng: [evidence/pa4-q3-retest/](evidence/pa4-q3-retest/).
- Lượt 1: ảnh `RETEST-*`, `run-log.txt`.
- Lượt 2: ảnh `RETEST2-*`, `run-log-6ad7f73.txt`.

Thư mục này nằm trong PR #20 và chỉ có trên `main` sau khi PR #20 được merge.

Ghi nhận khi retest: sau khi dựng lại image, trình duyệt vẫn dùng `index.html` cũ trong cache, phải tải lại kèm tham số mới thấy bundle mới. Có thể do nginx không gửi `Cache-Control` cho `index.html`. Đây không phải lỗi của B01–B03; nên xem xét trong task riêng.

## 6. Ghi chú và giới hạn

1. **Bằng chứng test tự động.** `TC-P04-07`, `TC-P05-06`, `TC-P05-10` chỉ có bằng chứng này, chưa chạy qua API/UI trong lượt demo; cần lưu ý khi tổng hợp kết quả. Phần lỗi truy xuất của `TC-P04-10`, `TC-P07-08` và phần quá cutoff của `TC-P04-08`, `TC-P07-06` cũng dựa trên lượt verify. Lượt verify được chạy lại trên đúng commit và đối chiếu theo tên test; đó không phải quan sát runtime tại mốc mili-giây.
2. **Ca chưa đủ bước UI:** TC-P05-02, TC-P05-04 cần mở trang chi tiết không qua tìm kiếm, nhưng SPA không có URL riêng; TC-P07-09 bước 3 cần làm chậm riêng một request. Đề xuất sửa ca ở Q1 (đưa phần đó về mức API/FE unit test) thay vì ghi Pass.
3. Expected TC-P04-01 ghi "tuyến DEMO-ABC"; giao diện hiển thị tên tuyến "Demo Route A-B-C" (DEMO-ABC là mã tuyến). Không tính là lỗi; nên chỉnh câu chữ trong ca.
4. Thông báo ngày quá khứ (TC-P04-04) là thông báo gốc của trình duyệt theo ngôn ngữ trình duyệt.
5. Chỉ thử trên Chromium (trình duyệt tích hợp, kèm giả lập mobile); chưa thử Edge, Safari và thiết bị thật như NFR01 nêu.
6. Kết quả CI/unit test không thay cho các ca chức năng; mục 4 ghi rõ ca nào dựa trên IT.

## 7. Việc tiếp theo

- Bản sửa B01–B03 đã có ở PR #20 (commit mới nhất `6ad7f73`) và retest đạt (mục 5.1). Việc còn lại:
  - Hoàng review PR #20.
  - Trung Hiếu điều phối merge.
  - Sau khi merge, QA ghi commit trên `main` và chuyển B01–B03 sang Closed.
  - Nếu PR #20 thay đổi thêm sau `6ad7f73`, retest lại các ca bị ảnh hưởng trên commit mới.
- Kết quả trên `pa4-v1.0` không đổi: hai ca Fail, ba lỗi tồn tại trên bản đó.
- Q1: chỉnh TC-P04-01 (tên tuyến), TC-P05-02, TC-P05-04, TC-P07-09 bước 3 theo ghi chú 2–3.
- Trung Hiếu duyệt báo cáo.
