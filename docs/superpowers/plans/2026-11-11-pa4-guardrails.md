# PA4 Guardrails Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to track the integration task by task; the user's scope is local integration only, without push or merge.

**Goal:** Tích hợp bộ PA4 guardrails thật vào repo, giữ dữ liệu có sẵn và báo đúng khả năng kiểm chứng.

**Architecture:** Giữ `src/`, `docs/`, `pa/`. Dùng `src/frontend` và `src/backend` làm đường dẫn dự kiến; thiếu source phải làm CI lỗi rõ ràng. Không tạo ứng dụng hoặc test giả trong task này.

**Tech Stack:** Markdown, GitHub Actions YAML, Bash; baseline dự kiến React/TypeScript/Vite, Java 21/Maven, PostgreSQL.

**Spec:** Yêu cầu người dùng trong phiên 11/11/2026; `CODEX_HANDOFF.md` và bộ `PA4_Repo_Guardrails` là tài liệu tham khảo, không tự mở rộng quyền push/merge/settings.

## Global Constraints

- Giữ nguyên thay đổi hiện có; không reset/clean/ghi đè file có sẵn.
- Không dùng script giả hoặc bỏ kiểm tra để CI xanh.
- Chưa push hoặc merge; không tự commit hoặc sửa settings remote.
- Phân biệt baseline được bàn giao, đề xuất triển khai và kết quả kiểm chứng thực tế.

## Review Focus

- Repo chỉ có `.gitkeep`: CI phải lỗi, không skip để xanh.
- Các quality jobs thất bại/skipped/cancelled: gate phải từ chối.
- Đường dẫn cache, working directory, docs và CODEOWNERS phải nhất quán với `src/`.
- Mẫu PostgreSQL/package/API không được báo thành implementation đã có.
- Kết quả local không được báo thành GitHub Actions hoặc test nghiệp vụ đã pass.

## Task 1: Tích hợp và điều chỉnh

- [x] Đọc handoff, tìm AGENTS kế thừa, kiểm tra status/remote/cây Git và bộ kit thật.
- [x] Tạo nhánh `chore/repo-guardrails`; chép đủ 10 file với kiểm tra không ghi đè.
- [x] Chỉnh contracts/agent/docs/CI cho bố cục `src/`; đánh dấu phần chưa có source.
- [x] Kiểm tra diff và các liên kết/tính nhất quán tài liệu.

## Task 2: Kiểm chứng và bàn giao

- [x] Parse YAML và lint workflow bằng actionlint thật.
- [x] Chạy chính đoạn kiểm tra đầu vào trong workflow: repo hiện tại phải lỗi và liệt kê đủ file thiếu.
- [x] Chạy đoạn gate với mọi tổ hợp success/failure/skipped/cancelled: chỉ success/success được pass.
- [x] Chạy `git diff --check`, rà thay đổi và ghi báo cáo đối chiếu/kết quả/giới hạn.

Không có lệnh build/test ứng dụng khả thi ở baseline rỗng; không thay bằng tests rỗng. Thay đổi chỉ là tài liệu/cấu hình, kiểm chứng trực tiếp thay vì thêm test mô phỏng implementation.
