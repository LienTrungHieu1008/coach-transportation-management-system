# Team 11 frontend

React + TypeScript + Vite + Tailwind. Dùng Node theo `.nvmrc` và npm lockfile.

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
npm.cmd run dev
```

Lint chạy ESLint, typecheck chạy TypeScript strict, tests chạy Vitest/Testing Library. Không dùng Oxlint hoặc script giả. Dev server chỉ bind loopback; `/api` proxy tới backend ở `http://localhost:8080`. API client dùng cùng origin, không gọi Gemini từ trình duyệt.

Đây là màn hình nền tảng với trạng thái health thực và retry, chưa phải UI prototype hoặc UC nghiệp vụ PA4. Khi có lỗi mạng/HTTP/dữ liệu health sai, giao diện báo chưa kết nối, không giả thành công.

Xem [hướng dẫn local/test/Compose](../../docs/development.md), [AGENTS](../../AGENTS.md) và [phạm vi PA4](../../pa/PA4/SCOPE.md).
