# FE PA4 Journey API Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Hoàn thiện FE tìm chuyến → chi tiết → ghế trên API thật để Hoàng review và Đức Hiếu cập nhật SAD.

**Architecture:** Giữ thiết kế F1, dùng client chung kiểm tra response; state hành trình quản lý tiêu chí đã submit và chuyến/Segment được chọn. Hook ghế phụ trách refresh và chống race; các page chỉ render state và gọi action, không lặp fetch.

**Tech Stack:** React/TypeScript/Vite/Tailwind, Vitest + Testing Library hiện có, Node 24.19.0; không thêm dependency.

**Spec:** `docs/superpowers/specs/2026-11-18-pa4-journey-api-integration.md`.

## Global Constraints

- Leader đã duyệt và giao triển khai ngày 18/11; không tự commit/push/merge.
- Phạm vi chỉ P04/P05/P07; giữ stack, contract, BE, migration, security và CI.
- Không tắt `erasableSyntaxOnly`, lint/typecheck/tests; không dùng any hoặc fallback mock trong app.
- Giữ thay đổi đang có ở workspace gốc; execution dùng worktree riêng qua using-git-worktrees.
- Nhánh đề xuất từ main mới nhất; chỉ tiếp nhận client #17, ghi nguồn tác giả trong bàn giao.
- Quyết định P4-01 và ẩn Lịch trình đã chốt; mang nội dung cần thiết vào PR thực thi,
  không kéo toàn bộ docs tiến độ chưa commit hoặc phụ thuộc worktree ẩn Lịch trình cũ.
- Sau mỗi task ghi diff/kết quả; commit/push/PR chỉ khi leader giao, không tự thực hiện.

## Review Focus

1. Người dùng đổi tiêu chí/trip khi request cũ đang chạy → response cũ không đổi màn mới (task 2/3).
2. Response 200 sai shape hoặc sai Trip/Segment → lỗi an toàn, không render facts sai (task 1/3/4).
3. P4 partial/inactive/null → xem được facts nhưng chặn sang ghế đúng eligibility (task 3).
4. Tab ẩn, máy lệch giờ, expiry và refresh lỗi → không tự chuyển FREE hoặc giữ stale FREE (task 4).
5. Nhiều trang, đủ A/B/C và ngày lên chuyến qua đêm → gửi đúng query, giữ thứ tự/metadata server (task 2/5).

## File structure dự kiến

Paths FE bên dưới tính từ `src/frontend/`:

- `src/shared/api/journeyTypes.ts`: DTO và paging options/page response.
- `src/shared/api/journey.ts`: tiếp nhận #17, sửa constructor và client signatures.
- Mới `src/shared/api/journeyValidation.ts`, `journey.test.ts`: guards/contract client tests.
- `src/features/journey/useJourneyState.ts`: tiêu chí nháp/submitted, navigation, API search/details.
- Mới `src/features/journey/useSeatAvailability.ts`, `useSeatAvailability.test.tsx`: vòng đời refresh.
- `src/features/journey/JourneyPages.tsx`: page rendering; giữ exports hiện có, không refactor ngoài nhu cầu.
- `src/App.tsx`, `src/App.test.tsx`: nối state/page, scope, tests flow.
- `src/features/journey/journey.css`: chỉ sửa cho paging/states/expiry/multi-deck khi cần.
- Mới `docs/analysis and design/pa4-ui-prototype.md` và `docs/test/fe-api-integration-handoff.md`.

## Task 1 — Client chạy được và xác minh response

**Files:** journeyTypes.ts, journey.ts, journeyValidation.ts, journey.test.ts.

**Interfaces:**
- `journeyApi.listStops(signal?: AbortSignal): Promise<StopOption[]>`.
- `journeyApi.searchTrips(criteria: SearchCriteria, options: {page: number; size: number}, signal?: AbortSignal): Promise<TripSearchPage>`; export TripSearchPage với items/page/size/totalElements/totalPages/serverTime theo OpenAPI.
- `journeyApi.getTripDetails(tripId: string, segment?: {fromStopId: string; toStopId: string}, signal?: AbortSignal): Promise<TripDetails>`.
- `journeyApi.getSeatAvailability(tripId: string, segment: {fromStopId: string; toStopId: string}, signal?: AbortSignal): Promise<SeatAvailability>`.
- Giữ JourneyApiError(message,status,body); khai báo readonly fields riêng và gán constructor.

- [x] Viết tests `rejectsMalformedSuccess`, `preservesPagingMetadata`, `allowsNullableDetails`, `rejectsMismatchedTripOrSegment`, `preservesAbortError`, `mapsSafeHttpErrors`.
  Assertions: 200 `{}` và `{items:[{}]}` bị từ chối; mọi field dùng render đúng kiểu;
  money safe integer không âm, ngày/instant hợp lệ; lỗi fieldErrors phải có field/message string;
  P4 null hợp lệ không bị từ chối; 503 giữ code và không trả dữ liệu mẫu.
- [x] Chạy `npm.cmd run typecheck` để ghi RED TS1294 trên client gốc; `npm.cmd test -- src/shared/api/journey.test.ts` để ghi RED hành vi. Không coi lỗi môi trường là RED đúng.
- [x] Sửa client và guards theo OpenAPI, kiểm tra enum/nullability/ID/layout-seats nhất quán; không thêm validator dependency. Không nuốt AbortError từ fetch hoặc response.json.
- [x] Chạy lại typecheck và file test; cần PASS. Ghi nguồn client #17 trong handoff.

## Task 2 — F2 danh mục, tìm chuyến và paging thật

**Files:** useJourneyState.ts, JourneyPages.tsx, App.tsx, App.test.tsx.

**Interfaces:** dùng client task 1. Hook trả page, draftCriteria, submittedCriteria,
searchPage, selectedTrip, loading/error states và actions `setDraftCriteria`,
`submitSearch`, `changeSearchPage`, `selectTrip`, `navigate`.
`submitSearch()` chụp tiêu chí, reset page=0; `changeSearchPage(page)` giữ submittedCriteria.
`selectTrip(item)` giữ nguyên tripId/fromStop.id/toStop.id để details dùng.

- [x] Thêm tests `loadsStopsFromApi`, `submitsAllThreeStopsWithoutHardcodedRoute`,
  `paginatesWithSubmittedCriteria`, `ignoresOutOfOrderSearch`, `showsEmptyAndRetry`.
  Fixture A/B/C; page 0 có 20 items, totalElements=21/totalPages=2; page 1 có 1 item.
  Sửa draft sau submit không đổi query paging. Deferred response cũ trả sau không ghi đè response mới.
- [x] Chạy `npm.cmd test -- src/App.test.tsx` xác nhận RED đúng các chức năng mới.
- [x] Load Stop thật; mặc định ngày hiện tại VN, bỏ giới hạn demo A→B/ngày cố định.
  Render items, paging/total và sort server; giữ loading/rỗng/lỗi/retry, không fallback demoTrip.
  Client chỉ kiểm tra cặp khác nhau/ngày đủ format; nghiệp vụ để server kiểm tra.
- [x] Dùng request key + sequence và AbortController khi đổi tiêu chí/rời màn;
  tests phải PASS, không loại StrictMode để né request lifecycle.

## Task 3 — F3 chi tiết, partial details và giữ Segment

**Files:** useJourneyState.ts, JourneyPages.tsx, App.test.tsx.

**Interfaces:** state `details: TripDetails | null`, detailsLoading/detailsError;
`loadDetails(tripId, segment?)`, `openSeats()` chỉ được chạy khi details đúng selection,
selectedSegment có giá trị và bookingEligibility.allowed=true.

- [x] Tests `loadsSelectedTripAndSegment`, `displaysPartialDetailsWithoutInventingFacts`,
  `blocksInactiveRouteOrStop`, `ignoresOldDetailsResponse`, `handles404And503`.
  A–C giá 250000; thiếu B–C nhưng hỏi A–B vẫn 100000 và essential incomplete/disabled;
  description null không chặn nếu allowed=true; route/vehicle null không crash.
- [x] Chạy tests để ghi RED; triển khai load details từ IDs task 2, hiển thị tên từ facts đã xác minh,
  không thay giá/giờ null bằng 0 hoặc timestamp khác. Hiển thị toàn bộ reason bằng tiếng Việt.
- [x] Request sai Trip/Segment không được render; đổi selection hủy/bỏ qua request trước.
  Không chọn segment: hiển thị yêu cầu chọn; luồng điều hướng hiện tại chọn qua tìm kiếm.
- [x] Chạy lại App tests, typecheck; PASS mới sang task 4.

## Task 4 — F4 ghế thật, refresh và chống stale/race

**Files:** useSeatAvailability.ts, useSeatAvailability.test.tsx, JourneyPages.tsx, App.tsx, journey.css.

**Interfaces:**
`useSeatAvailability(selection: {tripId: string; fromStopId: string; toStopId: string} | null)`
trả `{data: SeatAvailability | null, loading: boolean, error: JourneyApiError | Error | null, stale: boolean, refresh: () => void}`.
Chỉ bật selection khi ở Seats và details hợp lệ. Hook cleanup timer/listener/request khi key đổi hoặc unmount.

- [x] Tests bằng fake timers/deferred fetch: `pollsVisibleTabEvery15Seconds`,
  `pausesHiddenAndRefreshesOnForeground`, `refreshesAtExpiryAndCutoffUsingServerOffset`,
  `neverFreesSeatsFromTimer`, `marksStaleOnErrorOrElapsedExpiry`, `ignoresOldMap`,
  `cleansUpOnUnmount`, `handles409WithoutFakeFree`.
  Cho giờ máy lệch server 2 giờ; lịch dùng khoảng expiry/cutoff trừ serverTime,
  không quyết định status theo Date.now máy; tiến timer không tự đổi UNAVAILABLE thành FREE.
- [x] Chạy `npm.cmd test -- src/features/journey/useSeatAvailability.test.tsx` ghi RED.
- [x] Implement GET ban đầu, 15s foreground, manual và timer expiry/cutoff; không chạy
  request chồng cùng selection. Nếu trigger lúc pending, gộp một refresh kế tiếp;
  request quá hạn/rời selection phải hủy hoặc mất quyền cập nhật. Lỗi/abort phân biệt.
- [x] Render theo physicalSeatId/layout deck-row-column, label/expiry đọc được trên mobile;
  phản hồi quá hạn/refresh fail hiển thị UNKNOWN hoặc đang xác minh, không để stale FREE.
  Toàn bộ UNAVAILABLE vẫn là kết quả 200 bình thường. 409 chặn và cho quay lại details/search.
- [x] Tests PASS; không thêm Booking/hold, chọn ghế hoặc công tắc ép lỗi production.

## Task 5 — Chốt scope, regression và runtime thật

**Files:** App.tsx, App.test.tsx, JourneyPages.tsx, journey.css; chỉ cập nhật docs scope cần thiết.

- [x] Tests `hidesScheduleNavigation`, `hasNoPrototypeErrorSwitches`,
  `completesSearchDetailsSeatsWithSameIds`, `rendersSixSeatsAcrossDecks`.
  Thay tests cũ bấm preview bằng mock HTTP tương ứng; giữ ý nghĩa validation/loading/error/back.
- [x] Ẩn Schedule theo quyết định leader; bỏ mock labels/control khỏi luồng chạy thật,
  không để nội dung marketing ghi giá/tuyến mẫu như dữ liệu server. Không mở rộng thiết kế.
- [x] Chạy từ `src/frontend`: `npm.cmd ci`, `npm.cmd run lint`, `npm.cmd run typecheck`,
  `npm.cmd test`, `npm.cmd run build`. Ghi kết quả thật và audit findings; không auto-fix dependencies.
- [x] Dựng Compose cùng origin/DB demo riêng theo docs development và README Booking trên main;
  nếu tool/DB chưa sẵn thì báo chưa kiểm chứng, không lấy mocks thay bằng chứng runtime.
- [x] Kiểm tra HTTP/UI: A–B=100000, A–C=250000, B–C=150000; đủ sáu ghế, A2 tái dùng trên B–C;
  B2 còn hold/hết hạn; P4 eligibility chặn; empty/error/retry/back; ngày lên chuyến qua đêm;
  GET không thêm Booking. Kịch bản thiếu dữ liệu #15 dùng DB riêng không Booking.
- [x] Rà desktop/mobile, keyboard/focus, không tràn ngang, expiry không chỉ tooltip;
  lưu ảnh/bằng chứng sạch, không commit logs/build output/secret.

## Task 6 — Bàn giao Hoàng và Đức Hiếu

**Files:** docs/analysis and design/pa4-ui-prototype.md, docs/test/fe-api-integration-handoff.md;
chỉ cập nhật ownership trong pa/PA4/PROGRESS.md theo kết quả thực tế.

- [x] Mô tả bốn màn: mục đích/thông tin/cách dùng, ảnh, luồng lỗi và giới hạn đúng bản đã chạy.
- [x] Handoff ghi phần tiếp nhận từ #17, phần leader hoàn thiện, API/state flow, tests,
  runtime đã/chưa chạy, cách seed/chạy; ghi path để Hoàng đọc code và giải thích một nhánh lỗi.
- [x] Ghi các owner đã gửi SAD/weekly theo xác nhận leader; phần FE đang leader nhận hoàn thiện,
  chưa ghi Hoàng đã review hoặc Đức Hiếu đã nhận phần bổ sung khi chưa có bằng chứng.
- [ ] Sau khi leader giao push/PR: tạo PR từ nhánh mới, mô tả đúng diff, Hoàng review/tiếp nhận;
  phối hợp xử lý #17 sau khi phần cần thiết đã được nhận, không tự đóng PR đó.
- [ ] Sau review và kết quả thực tế, gửi phần FE bổ sung cho Đức Hiếu cập nhật SAD;
  không giao gửi lại nội dung các module đã bàn giao. Không tự merge.

## Self-review và trạng thái

- Các yêu cầu spec đều có task: client(1), search(2), details(3), refresh(4), scope/runtime(5), docs/review(6).
- Không cần backend mới; main đã có API. Chưa giao sửa #15 hoặc audit trong kế hoạch FE.
- Kiểm tra plan mới là đọc source/contract, không phải đã thực thi các bước hoặc tests bên trên.
- Đã thực thi trực tiếp trong worktree riêng; có một review code độc lập cuối theo
  executing-plans. Hoàng vẫn là reviewer bàn giao của nhóm, không ghi đã tiếp nhận.
- Kết quả/giới hạn: `docs/test/fe-api-integration-handoff.md`; ledger ở tmp được ignore.
