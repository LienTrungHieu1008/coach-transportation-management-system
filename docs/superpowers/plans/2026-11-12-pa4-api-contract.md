# PA4 API Contract Plan

> **For agentic workers:** This task delivers documentation only; execute inline and review independently. Product implementation belongs to separate member tasks after contract review.

**Goal:** Cụ thể hóa bốn GET đã được người giao việc đồng ý thành OpenAPI và port để cả nhóm review trước khi code riêng.

**Architecture:** Same-origin REST `/api/v1`, Trip sở hữu ba API/dữ liệu chuyến; Booking sở hữu availability và dùng TripQueryPort. Chưa sửa source, migration, dependency ứng dụng hoặc CI/security.

**Tech Stack:** OpenAPI 3.1, JSON Schema Draft 2020-12, YAML/JSON; nền tảng React/Java/PostgreSQL giữ nguyên.

**Spec:** `docs/contracts/pa4-api-proposal.md`, UC 1.8 P04/P05/P07 và SAD 1.3; cấu trúc bốn GET được người dùng đồng ý ngày 12/11/2026.

## Constraints and review focus

- Ghi rõ cấu trúc đã duyệt và chi tiết còn là review candidate, không tự nhận đã có API runtime.
- Giữ public GET, UUID/camelCase, UTC/VND integer, ngày Stop lên, cutoff đầu tuyến, Segment nửa mở.
- Không giả FREE khi không tải được ghế, không gia hạn hold qua refresh và không bỏ chuyển Pending → Expired.
- Examples là dữ liệu minh họa, không seed/fixture production và không bằng chứng functional tests.
- Kiểm tra required/null, enum/status, paging, ID/layout mapping và thời gian/giá ví dụ.

## Tasks

- [x] Đọc repo/contracts và đối chiếu UC gốc; nhận đồng ý cấu trúc bốn GET.
- [x] Viết `docs/contracts/openapi.yaml`, bảy JSON trong `docs/contracts/examples/`, `trip-query-port.md`, `pa4-api-decisions.md`.
- [x] Validate OpenAPI và examples với format checking; kiểm tra chéo dữ liệu ví dụ, refs và whitespace.
- [x] Review độc lập, sửa vấn đề và cập nhật nguồn chỉ dẫn/trạng thái.
- [x] Hoàn thiện bộ tài liệu để commit/push cùng nhánh review đã được giao; bàn giao các lựa chọn cần nhóm review. CI của commit mới được đối chiếu theo SHA trên PR #1 khi báo kết quả.
