# PA4 — Kế hoạch kiểm thử (Test Plan) Nhóm 11

**Task:** Q1 · **Người soạn:** Nguyễn Đức Hiếu (QA) · **Ngày soạn:** 17/11/2026 · **Phiên bản:** 0.1 (bản đầu, chưa được rà soát)

**Trạng thái:** đây là **thiết kế kiểm thử**, chưa thực thi ca nào. Mọi cột Actual/Status trong [ma trận ca](pa4-test-cases.md) đang để trống theo quy ước "chưa chạy không đánh dấu pass". Kết quả thực thi thuộc task Q3 (19–20/11), báo cáo riêng.

---

## 1. Định danh kế hoạch (Test plan identifier)

`PA4-Group11-TESTPLAN-v0.1`

Tài liệu đi kèm:

| Tài liệu | File | Trạng thái |
|---|---|---|
| Kế hoạch kiểm thử | `docs/test/pa4-test-plan.md` (file này) | Bản đầu 17/11 |
| Ma trận ca kiểm thử | [`docs/test/pa4-test-cases.md`](pa4-test-cases.md) | Bản đầu 17/11, 30 ca |
| Ca kiểm thử theo mẫu Moodle | `docs/test/PA4-Group11-test-cases.xlsx` | Đã điền phần thiết kế; Actual/Status/Tester/Date để trống |
| Báo cáo kiểm thử | chưa tạo | Thuộc Q3, dùng `test-report template-v1.1.xlsx` |

Mẫu gốc lấy từ gói tài liệu môn học `2627-CQ2024_4/PA4/` (`test-cases template-v1.1.xlsx`, `test-report template-v1.1.xlsx`). Gói này **chưa có mẫu test plan riêng**, nên bố cục dưới đây theo các mục kiểm thử trong bài giảng/IEEE 829. Nếu trợ giảng cung cấp mẫu test plan khác, chuyển nội dung sang đúng mẫu, không viết lại nội dung.

## 2. Giới thiệu (Introduction)

Hệ thống: **Smart Bus Booking** — ứng dụng web đặt vé xe khách (SPA React + backend Spring Boot + PostgreSQL 16).

PA4 chỉ hiện thực và kiểm thử một luồng công khai liên tục, không đăng nhập/thanh toán/AI:

> Tìm chuyến (UC-P04) → Xem chi tiết chuyến (UC-P05) → Xem ghế trống theo Segment (UC-P07)

Mục tiêu kiểm thử:

1. Xác nhận luồng chính của ba UC chạy đúng đặc tả trên dữ liệu thật trong PostgreSQL (không phải mock/JSON hardcode).
2. Xác nhận các nhánh thay thế (A1–A4 của từng UC) và business rules BR-P04-01…05, BR-P05-01…03, BR-P07-01…04.
3. Phát hiện và ghi nhận lỗi, trả về đúng owner module, retest sau khi sửa.

Nguồn yêu cầu: [UC Spec v1.8](../requirements/Team%2011%20-%20rup_ucspec-v2_final.pdf) (P04 tr.10–12, P05 tr.12–13, P07 tr.15–16), [SAD PA3 v1.3](../analysis%20and%20design/PA3_Group11_SAD_v1.3_final.pdf), [C1_DECISIONS](../../pa/PA4/C1_DECISIONS.md), [OpenAPI 0.1](../contracts/openapi.yaml), [TripQueryPort](../contracts/trip-query-port.md), [manifest dữ liệu demo P1](p1-trip-demo-manifest.md).

Khi tài liệu mâu thuẫn, thứ tự ưu tiên: **UC Spec → C1 → OpenAPI → tài liệu bàn giao của owner**. Mâu thuẫn phát hiện trong lúc kiểm thử báo leader, không tự sửa expected cho khớp implementation.

## 3. Hạng mục kiểm thử (Test items)

| Hạng mục | Thành phần | Owner |
|---|---|---|
| FE | `src/frontend` — bốn màn: Trang chủ/tìm chuyến, Kết quả, Chi tiết chuyến, Sơ đồ ghế (F1–F4) | Khánh Hoàng |
| API Trip | `GET /api/v1/stops`, `GET /api/v1/trips`, `GET /api/v1/trips/{tripId}` (P2/P4) | Minh Phương |
| API ghế | `GET /api/v1/trips/{tripId}/seat-availability` (H2) + job dọn hold hết hạn (H3) | Nhật Huy |
| Dữ liệu | Migration V1→V2→V3→V4 và seed demo (`trip_demo_runtime.sql`, fixture Booking của H1) | Trung Hiếu (V2), Phương (V3), Huy (V4) |
| Tích hợp | Security/error mapping chung, luồng FE–BE (C2) | Trung Hiếu |

Build được kiểm thử sẽ ghi bằng **commit SHA** cụ thể trong báo cáo Q3; kế hoạch này không gắn với một build nào.

## 4. Chức năng cần kiểm thử (Features to be tested)

| ID | Chức năng | Số ca | Ưu tiên |
|---|---|---:|---|
| UC-P04 | Tìm chuyến theo Stop lên/xuống và ngày lên | 10 | Cao |
| UC-P05 | Xem chi tiết chuyến và điều kiện đặt chỗ | 10 | Cao |
| UC-P07 | Xem trạng thái ghế theo Segment | 10 | Cao |

Tổng **30 ca**, vượt yêu cầu tối thiểu 15 ca (≥5 ca/UC) của đề PA4.

Các điểm bắt buộc phải có ca kiểm thử:

- Luồng chính (basic flow) của cả ba UC.
- Nhánh thay thế: P04 A1/A2/A3; P05 A1/A2/A3/A4; P07 A1/A2/A3/A4.
- Quy tắc ngày theo `Asia/Ho_Chi_Minh` kể cả chuyến qua đêm (BR-P04-05).
- Cutoff 30 phút trước giờ rời Stop đầu, áp dụng cho mọi Segment, đúng mốc là đóng (BR-P04-02).
- Giá Segment = tổng leg liên tiếp, BE tính (BR-P04-03, BR-P05-03).
- Quy tắc chặn ghế: chỉ Confirmed và hold chưa hết hạn, đoạn kề không giao nhau (BR-P07-02).
- Xem ghế không tạo Booking/hold (BR-P07-04).
- Không biến dữ liệu không xác minh được thành FREE/giá 0 (C1).

## 5. Chức năng không kiểm thử (Features not to be tested)

| Không kiểm thử | Lý do |
|---|---|
| UC-P01/P02 đăng ký, đăng nhập | Ngoài phạm vi PA4 đã chốt; V2 chỉ tạo schema Account làm prerequisite FK, không có API login |
| UC-P08/P09 tạo Booking, thanh toán | Không hiện thực trong PA4 |
| UC-P11 gợi ý chuyến bằng AI | Không hiện thực trong PA4 |
| Màn hình/API quản trị, nhân viên | Ngoài phạm vi |
| Kiểm thử hiệu năng, tải, bảo mật chuyên sâu, khả dụng (accessibility) | Không yêu cầu ở PA4; chỉ ghi nhận quan sát nếu phát hiện |
| Health endpoint, CSRF, Spring Session | Thuộc bộ khung, đã kiểm chứng ở [foundation-verification](foundation-verification.md); **không tính vào 15 ca chức năng PA4** |

## 6. Cách tiếp cận (Approach)

**Kỹ thuật:** kiểm thử chức năng hộp đen (functional/black-box) ở mức giao diện người dùng, đối chiếu với response API khi cần xác định lỗi thuộc FE hay BE.

Kỹ thuật thiết kế ca áp dụng:

- **Phân hoạch tương đương:** ngày tìm (quá khứ / hôm nay / tương lai có chuyến / tương lai không chuyến), cặp Stop (hợp lệ / trùng / ngược chiều / không phục vụ), trạng thái Trip (Scheduled / Cancelled / Departed / Completed).
- **Giá trị biên:** mốc cutoff (cutoff−1 phút, đúng cutoff, cutoff+1 phút), hạn hold (trước/đúng/sau `expiresAt`), ranh giới ngày 00:00 giờ Việt Nam với chuyến qua đêm.
- **Kiểm thử theo trạng thái:** ma trận ghế × loại allocation (Confirmed trùng đoạn, Confirmed đoạn kề, hold còn hiệu lực, hold hết hạn, allocation đã huỷ/giải phóng).
- **Kiểm thử nhánh lỗi:** mã lỗi và HTTP status theo OpenAPI (400/404/409/422/503).

**Mức thực thi:** mỗi ca ghi rõ thực thi ở mức nào.

| Mức | Ý nghĩa | Dùng khi |
|---|---|---|
| `UI` | Thao tác trên trình duyệt với FE nối API thật | Mặc định cho luồng người dùng |
| `UI+API` | Thao tác UI, kèm kiểm tra response/status bằng công cụ HTTP hoặc DevTools | Khi cần phân biệt lỗi FE/BE hoặc xác nhận mã lỗi |
| `API` | Gọi trực tiếp endpoint | Ca chỉ quan sát được ở mức contract (ví dụ 400 do tham số sai định dạng) |
| `DB` | Truy vấn đọc trên DB demo | Xác nhận không có Booking/hold được tạo |
| `IT` | Dựa vào integration test của owner module | Ca lỗi hạ tầng (503) — theo C1 **không** được thêm switch giả lỗi vào API public, nên QA không tự tạo được tình huống này ở runtime |

Ca mức `IT` vẫn thuộc trách nhiệm QA xác minh: QA đọc tên test, chạy lại lệnh verify của owner và ghi kết quả/commit vào báo cáo; **không ghi pass chỉ vì owner nói đã có test**.

**Dữ liệu kiểm thử:** dùng bộ demo chung do Phương giữ manifest, Huy bổ sung allocations; toàn bộ là dữ liệu tổng hợp, không có dữ liệu khách hàng. Chi tiết ở mục 11.

**Không chấp nhận:** sửa expected theo kết quả quan sát được, bỏ ca khó, dùng kết quả unit/integration test thay cho ca chức năng của UC, hoặc dùng dữ liệu mock của FE làm bằng chứng nghiệm thu.

## 7. Tiêu chí đạt/không đạt từng ca (Item pass/fail criteria)

- **Pass:** mọi điểm trong cột Expected Result xảy ra đúng, kể cả mã lỗi, thứ tự sắp xếp và giá trị số (giá VND, số ghế).
- **Fail:** bất kỳ điểm nào sai, thiếu hoặc không xác minh được. Ghi Actual thực tế, tạo defect, gán owner theo bảng mục 12.
- **Blocked:** không chạy được vì phụ thuộc chưa sẵn sàng (API chưa có, fixture chưa có, môi trường lỗi). Ghi rõ đang chờ gì/ai; **không ghi Fail cho phần chưa tồn tại và không ghi Pass cho phần chưa chạy**.
- **N/A:** ca không còn áp dụng do thay đổi phạm vi đã được leader chốt; ghi lý do.

Một ca chỉ được đổi sang Pass sau khi chạy lại trên bản sửa (retest), ghi commit mới.

## 8. Tiêu chí vào/ra, tạm dừng và chạy lại

**Tiêu chí vào (entry):**

1. Build FE+BE chạy được bằng Docker Compose, ghi rõ commit.
2. Migration V1→V4 áp dụng thành công trên DB demo sạch.
3. Seed dữ liệu demo đã nạp và xác minh theo [seed guide](p1-trip-runtime-seed-guide.md).
4. Endpoint tương ứng của ca đã được ghép security (GET public không bị 401/403).

**Tiêu chí ra (exit) cho PA4:**

1. 100% ca đã chạy ít nhất một lần, mỗi ca có Actual và Status.
2. Không còn defect mức Critical/High ở trạng thái Open trong luồng P04→P05→P07; defect Medium/Low còn lại được ghi rõ trong báo cáo kèm lý do chưa sửa.
3. Báo cáo kiểm thử ghi đủ: UC đã kiểm thử, môi trường (trình duyệt/thiết bị), commit, dữ liệu, kết quả và danh sách lỗi.

**Tạm dừng (suspension):** dừng đợt chạy khi môi trường không ổn định (API 5xx hàng loạt, DB không seed được, FE không build) hoặc khi hơn 30% ca trong một UC Blocked vì cùng một nguyên nhân. Báo ngay owner liên quan và leader.

**Chạy lại (resumption):** tiếp tục khi nguyên nhân chặn đã sửa, có commit mới và seed lại dữ liệu sạch. Các ca đã chạy trước đó trên build cũ phải chạy lại nếu commit chạm module tương ứng.

## 9. Sản phẩm bàn giao (Test deliverables)

| Sản phẩm | Thời điểm | Người làm |
|---|---|---|
| Test plan (file này) | 17/11 bản đầu; cập nhật khi chốt môi trường | Đức Hiếu |
| Ma trận ca + file theo mẫu Moodle | 17/11 bản đầu; chủ UC rà expected trước 18/11 | Đức Hiếu soạn; Phương/Huy/Hoàng rà |
| Dữ liệu/cách seed cho từng kịch bản | Trước 18/11 | Phương (Trip), Huy (Booking/hold) |
| Kết quả thực thi (Actual/Status/bằng chứng) | 19–20/11 trước 12h | Đức Hiếu |
| Bug report theo mẫu | Trong lúc thực thi | Đức Hiếu ghi; owner sửa |
| Báo cáo kiểm thử tổng hợp | 20/11 trước 12h | Đức Hiếu; Trung Hiếu duyệt |

## 10. Công việc kiểm thử (Testing tasks)

1. **Q1 (xong bản đầu 17/11):** đọc UC/BR/C1/OpenAPI, thiết kế ca, lập ma trận truy vết UC/BR ↔ ca.
2. **Rà soát expected (18/11):** Phương rà ca P04/P05, Huy rà ca P07, Hoàng rà phần UI; ghi ý kiến vào PR của nhánh `docs/q1-test-plan`.
3. **Chuẩn bị môi trường (19/11):** dựng Compose, seed dữ liệu demo, kiểm tra endpoint public truy cập được.
4. **Thực thi vòng 1 (19/11 → 20/11 sáng):** chạy đủ 30 ca, ghi Actual, chụp bằng chứng.
5. **Ghi defect và giao owner (ngay khi phát hiện).**
6. **Retest (20/11 trước 20h):** chạy lại ca Fail trên bản sửa; cập nhật trạng thái.
7. **Báo cáo (20/11 trước 12h bản chính, cập nhật sau retest):** tổng hợp theo mẫu test-report.

## 11. Môi trường kiểm thử (Environmental needs)

**Môi trường chạy:**

| Hạng mục | Giá trị dự kiến |
|---|---|
| Cách chạy | `docker compose up --build -d --wait` từ gốc repo; UI và API cùng origin tại `http://localhost:8088` |
| Backend | Java 21, Spring Boot, Maven Wrapper |
| Database | PostgreSQL 16, **DB demo tách riêng**, không dùng DB dùng chung/thật |
| Trình duyệt | Chrome (bản hiện hành) làm chính; Microsoft Edge làm phụ; kiểm tra bố cục ở khung nhìn mobile 390×844 |
| Công cụ phụ | DevTools Network, `curl`/PowerShell `Invoke-WebRequest` cho ca mức API, `psql` cho ca mức DB (chỉ truy vấn đọc) |
| Múi giờ máy kiểm thử | `Asia/Ho_Chi_Minh` (ghi rõ nếu khác, vì ảnh hưởng ca ngày/giờ) |

Phiên bản thực tế (trình duyệt, Docker, commit) ghi trong báo cáo Q3, không ghi trước ở kế hoạch.

**Dữ liệu kiểm thử (theo manifest P1 + fixture H1):**

| Đối tượng | Giá trị |
|---|---|
| Route | `DEMO-ABC`, ba Stop theo thứ tự A(0) → B(1) → C(2) |
| Stop A / B / C | `2222…2222` / `3333…3333` / `9999…9999` |
| Trip ngày | `1111…1111`: A 09:00, B 11:00, C 13:00 (giờ VN ngày D) |
| Trip qua đêm | `1111…1112`: A ngày D 22:30, B ngày D+1 00:30, C ngày D+1 02:30 |
| Giá leg | A–B 100.000đ; B–C 150.000đ; A–C 250.000đ (BE cộng) |
| Cutoff | Trip ngày: D 08:30; Trip qua đêm: D 22:00 (giờ VN) |
| Xe/ghế | `DEMO-VEHICLE-01`, 6 ghế A1, A2, B1, B2, C1, C2 — dùng UUID trong manifest, **không suy ID từ nhãn/vị trí** |
| Ghế theo C1 | ghế 1 trống; ghế 2 Confirmed A–B; ghế 3 Confirmed A–C; ghế 4 hold B–C còn hiệu lực; ghế 5 hold đã hết hạn; ghế 6 allocation đã huỷ/giải phóng |

**Bộ dữ liệu phụ cần owner cung cấp trước khi chạy** (hiện chưa có đủ, xem mục 15):

| Bộ | Nội dung | Owner |
|---|---|---|
| `trip-not-sellable` | Trip Cancelled / Departed / Completed và Trip Scheduled đã qua cutoff | Phương |
| `incomplete-trip` | Trip thiếu dữ liệu essential (thiếu leg fare hoặc thiếu layout) | Phương |
| `all-seats-unavailable` | Mọi ghế bị chặn trên Segment kiểm thử | Huy |
| `hold-expiring` | Hold có `expiresAt` rơi vào khoảng đang xem (C1: `createdAt = seedTime−8 phút`, `expiresAt = seedTime+2 phút`) | Huy |

Bộ dữ liệu lỗi **không** được nạp chung với bộ demo tìm chuyến bình thường (C1).

## 12. Trách nhiệm (Responsibilities)

| Vai trò | Người | Trách nhiệm |
|---|---|---|
| QA | Nguyễn Đức Hiếu | Thiết kế ca, thực thi, ghi defect, retest, tổng hợp báo cáo |
| Chủ UC-P04/P05 | Đỗ Minh Phương | Rà expected ca P04/P05, cung cấp cách seed/reset, sửa bug module Trip |
| Chủ UC-P07 | Trương Đình Nhật Huy | Rà expected ca P07, cung cấp fixture ghế/hold, sửa bug module Booking |
| Giao diện | Nguyễn Khánh Hoàng | Cung cấp cách thử UI, sửa bug hiển thị/điều hướng, rà phần UI của ca |
| Tích hợp/leader | Liên Trung Hiếu | Môi trường tích hợp, security/error chung, duyệt hồ sơ cuối |

QA **không** sửa code module của người khác; defect trả về owner theo bảng trên.

## 13. Nhân sự và đào tạo (Staffing and training)

Một QA kiêm nhiệm (Đức Hiếu) cho toàn bộ PA4. Kỹ năng cần: đọc UC/BR, dùng DevTools, gọi API bằng curl/PowerShell, truy vấn đọc PostgreSQL, chạy Docker Compose. Không cần đào tạo thêm; phần phụ thuộc hiểu biết module do owner hướng dẫn khi bàn giao.

## 14. Lịch (Schedule)

Theo [deadline nội bộ đã chốt](../team-coordination.md):

| Mốc | Nội dung | Trạng thái |
|---|---|---|
| 16/11 | Bản đầu Q1 | **Trễ 1 ngày** — nộp 17/11 |
| 18/11 | Chủ UC rà expected; API P2/P4/H2/H3 sẵn sàng | Chưa |
| 19/11 | Luồng FE–BE chạy xuyên suốt (C2); QA bắt đầu thực thi | Chưa |
| 20/11 trước 12h | Chạy đủ ca, có actual/bằng chứng/bug; hồ sơ | Chưa |
| 20/11 trước 20h | Sửa bug, retest, chốt gói | Chưa |

## 15. Rủi ro và phương án (Risks and contingencies)

| Rủi ro | Ảnh hưởng | Phương án |
|---|---|---|
| Thời gian thực thi chỉ còn ~1,5 ngày sau khi luồng xuyên suốt xong (19/11) | Không chạy kịp 30 ca | Chạy theo thứ tự ưu tiên P1 → P2 trong ma trận ca; ca P2 chưa chạy ghi "chưa chạy", không ghi pass |
| API ghế (H2) và job expiry (H3) chưa có tại 17/11 | Toàn bộ ca P07 Blocked | QA chuẩn bị sẵn ca và script gọi; Huy báo ngay khi endpoint sẵn sàng |
| Bộ dữ liệu phụ (`incomplete-trip`, `all-seats-unavailable`, `hold-expiring`) chưa tồn tại | 8 ca không chạy được | Yêu cầu owner cung cấp trước 18/11; nếu không có, ghi Blocked kèm người đang chờ |
| Ca 503 không tạo được từ API public (theo C1 không thêm switch giả lỗi) | Không kiểm chứng được ở mức UI | Chuyển sang mức `IT`: QA chạy lại integration test của owner và ghi commit/kết quả |
| Ca cutoff/hold phụ thuộc thời gian thực | Khó tái hiện đúng mốc | Ưu tiên clock cố định trong test của owner; ở runtime seed lại dữ liệu với giờ phù hợp, ghi rõ giờ chạy |
| Seed runtime không idempotent (xung đột UUID khi chạy lại) | Mất thời gian dựng lại | Dùng DB demo dùng một lần, huỷ và tạo lại khi cần reset; không `TRUNCATE CASCADE`/`down -v` trên môi trường dùng chung |
| Mẫu test plan riêng của môn học có thể khác bố cục này | Phải trình bày lại | Giữ nguyên nội dung, chỉ chuyển sang mẫu khi nhận được |

## 16. Phê duyệt (Approvals)

| Vai trò | Người | Trạng thái |
|---|---|---|
| Soạn | Nguyễn Đức Hiếu | Xong bản 0.1, 17/11/2026 |
| Rà ca P04/P05 | Đỗ Minh Phương | **Chưa** |
| Rà ca P07 | Trương Đình Nhật Huy | **Chưa** |
| Rà phần UI | Nguyễn Khánh Hoàng | **Chưa** |
| Duyệt cuối | Liên Trung Hiếu | **Chưa** |

Tài liệu này không chứng minh phần mềm đã chạy đúng; nó chỉ mô tả cách nhóm sẽ kiểm thử.
