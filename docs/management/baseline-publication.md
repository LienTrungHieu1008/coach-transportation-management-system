# Bàn giao baseline lên GitHub — 12/11/2026

Người dùng đã giao hoàn thiện baseline sau khi xác nhận bước commit/push nhánh review. Đã thực hiện:

- Commit `efa1d0f7e95330796c5bc9f0271a7dc0403bbc2d`: bộ khung, guardrails/CI, tài liệu nguồn, phạm vi ba UC và phân công đề xuất. Maven wrapper được lưu mode executable cho Linux.
- Push nhánh `chore/repo-guardrails` lên origin, không force push.
- Mở [draft PR #1](https://github.com/LienTrungHieu1008/coach-transportation-management-system/pull/1) vào main để review.
- GitHub [Actions run 43254959247](https://github.com/LienTrungHieu1008/coach-transportation-management-system/actions/runs/43254959247) trên commit `efa1d0f`: `frontend-quality`, `backend-quality`, `team11-ci-gate` đều completed/success, xác nhận bằng API jobs. Frontend chạy npm ci/lint/typecheck/test/build; backend Maven verify với PostgreSQL 16 thật. Không nới/tắt kiểm tra.
- Các file chuẩn bị commit được kiểm tra whitespace, output build bị ignore, mẫu secret thông dụng và hash bốn PDF gốc. Đây không phải chứng nhận quét secret toàn diện. Kết quả kiểm thử local tại [foundation verification](../test/foundation-verification.md).

Cập nhật 14/11: `git fetch origin` xác nhận main tại `f3895f6`, merge PR #1 và chứa contract `bf2a3ae`. Thành viên lấy main mới để làm task. Không suy ra approval cá nhân hoặc enforcement settings từ commit merge; chưa xác minh lại branch protection/ruleset/CODEOWNERS trong lần cập nhật tài liệu này. Không deploy hoặc sửa repository settings.

Leader đã duyệt toàn bộ [C1](../../pa/PA4/C1_DECISIONS.md) ngày 14/11, dùng [OpenAPI 0.1](../contracts/openapi.yaml) và port để giao việc, chưa có implementation. [TASKS](../../pa/PA4/TASKS.md) và [handbook](../team-handbook.md) là đầu vào nhận việc. CI từng commit/run phải kiểm tra riêng; kết quả baseline cũ không chứng minh commit tài liệu mới đã được chạy trên GitHub.
