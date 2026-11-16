# F1 Visual Refresh Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement inline. Không giao subagent cho task CSS này.

**Goal:** Cải thiện hierarchy và khả năng đọc của F1, giữ toàn bộ tương tác prototype.
**Architecture:** Token chung ở index.css, stylesheet journey thống nhất và JSX chỉ đổi trình bày. Không thay data/logic, contract, dependency hoặc CI.
**Tech Stack:** React 19, TypeScript, CSS, Vite hiện có.
**Spec:** `docs/superpowers/specs/2026-11-17-f1-visual-design.md`.

## Global Constraints

Giữ 4 màn + Schedule, nhãn mô phỏng, preview states, keyboard focus, dark mode và reduced-motion. Không commit/push/merge. Làm tại checkout/nhánh F1 sạch hiện có để người dùng xem trực tiếp trong IDE, không tạo worktree.

## Review Focus

Form mobile không tràn; chữ/UUID dài wrap; focus không mất trên select; màu disabled/error/unknown rõ; sidebar không che CTA. Kiểm tra ở trình duyệt nếu công cụ hoạt động, không dùng tests DOM để tuyên bố đã kiểm tra layout.

## Task 1 — trình bày F1

- [x] Chạy baseline 15 tests và kiểm tra git status.
- [x] Sửa `src/frontend/src/index.css` cho token, typography và focus.
- [x] Thay `src/frontend/src/features/journey/journey.css` bằng stylesheet thống nhất cho shell, Home, Results, Details, Seats, Schedule, responsive/dark mode.
- [x] Chỉ sửa JSX để minh họa/nhãn trình bày nếu cần; giữ handler/DTO/state.
- [x] Chạy npm ci/lint/typecheck/test/build và git diff --check; không skip hoặc giảm kiểm tra.
- [x] Xem 4 màn desktop/mobile nếu môi trường cho phép, ghi kết quả và giới hạn vào báo cáo.

## Nhật ký

- Đã đọc UI UX Pro Max; tra design-system travel/transport và stack React. Chọn palette navy/cam phù hợp thay vì áp dụng máy móc gợi ý funnel quảng cáo hoặc thêm Google Fonts.
- Công cụ CUA chưa khởi tạo được do ổ C đầy; npm dùng TEMP/cache ổ D. Không thay cấu hình repo để né lỗi.

- Ho?n t?t: 15/15 tests; ci/lint/typecheck/build ??t. 20 t? h?p layout kh?ng tr?n; n?t >=44px; c? ?nh dark/error v? focus. B?o c?o: `docs/test/f1-visual-refresh-verification.md`. Kh?ng s?a JSX v? stylesheet ?? ?? cho thi?t k? ???c ch?n. Kh?ng commit/push.
