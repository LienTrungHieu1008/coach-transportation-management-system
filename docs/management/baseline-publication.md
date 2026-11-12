# Bàn giao baseline lên GitHub — 12/11/2026

Người dùng đã giao hoàn thiện baseline sau khi xác nhận bước commit/push nhánh review. Đã thực hiện:

- Commit `efa1d0f7e95330796c5bc9f0271a7dc0403bbc2d`: bộ khung, guardrails/CI, tài liệu nguồn, phạm vi ba UC và phân công đề xuất. Maven wrapper được lưu mode executable cho Linux.
- Push nhánh `chore/repo-guardrails` lên origin, không force push.
- Mở [draft PR #1](https://github.com/LienTrungHieu1008/coach-transportation-management-system/pull/1) vào main để review.
- GitHub [Actions run 43254959247](https://github.com/LienTrungHieu1008/coach-transportation-management-system/actions/runs/43254959247) trên commit `efa1d0f`: `frontend-quality`, `backend-quality`, `team11-ci-gate` đều completed/success, xác nhận bằng API jobs. Frontend chạy npm ci/lint/typecheck/test/build; backend Maven verify với PostgreSQL 16 thật. Không nới/tắt kiểm tra.
- Các file chuẩn bị commit được kiểm tra whitespace, output build bị ignore, mẫu secret thông dụng và hash bốn PDF gốc. Đây không phải chứng nhận quét secret toàn diện. Kết quả kiểm thử local tại [foundation verification](../test/foundation-verification.md).

Chưa merge, deploy, đổi repository settings, bật CODEOWNERS hoặc branch protection; chưa có approval độc lập của thành viên. Cần một thành viên khác review và leader xử lý merge trước khi nhóm tạo branch từ main mới. Nhánh baseline đã có thể fetch để đọc/chạy thử.

Bản [đề xuất API](../contracts/pa4-api-proposal.md) đang chờ trao đổi/chốt cùng người dùng; không xem là contract đã duyệt hay API đã triển khai. Commit tài liệu sau baseline không thay source/build/CI, nhưng CI của mỗi commit mới vẫn cần được kiểm tra riêng.
