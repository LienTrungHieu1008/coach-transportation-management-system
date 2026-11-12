# Application Foundation Implementation Plan

> **For agentic workers:** Execute locally; preserve the earlier uncommitted guardrails and all user changes. No commit/push/merge/settings changes were authorized.

**Goal:** Đưa tài liệu nguồn vào repo và dựng bộ khung chạy/kiểm thử được theo stack đã chốt.

**Architecture:** Một Spring Boot modular monolith + PostgreSQL 16; React/Vite phía trước cùng origin `/api` qua Nginx, Vite proxy cho dev. Session JDBC/Flyway và CSRF; chưa hiện thực UC nghiệp vụ. Dùng Docker Java 21 để không yêu cầu cài JDK lên máy host.

**Tech Stack:** Java 21, Spring Boot 3.5.16 (stable được xác nhận trên docs/Maven Central), Maven Wrapper từ Spring Initializr, PostgreSQL 16; Node 24.19.0 theo môi trường đã chạy, React/TS/Vite, Tailwind Vite plugin, ESLint và Vitest.

**Spec:** User approved adding original documents and building the foundation. SAD 1.3, UC 1.8 and PA4 references are indexed by `docs/references.md`; delivery limits in `pa/PA4/SCOPE.md`.

## Constraints and decisions

- Existing branch `chore/repo-guardrails`; earlier changes remain uncommitted and must survive.
- PDF originals byte-identical; Markdown is page-numbered text extraction, not an authoritative replacement for figures/tables.
- Real ESLint/typecheck/build, security tests and PostgreSQL integration tests; no empty tests, H2 substitution, skip flags or permissive CSRF workaround.
- Proposed module packages: `identity`, `trip`, `booking`, `ai` under `vn.edu.team11.coach`. Only identity's infrastructure/CSRF boundary is needed now; no pretend business modules.
- Health endpoint public and minimal; all other routes authenticated by default except CSRF bootstrap. No generated/default demo account, no business login yet.
- Docker image packaging runs unit tests; Maven `verify` also runs Failsafe integration tests against a separate real PostgreSQL test service. CI must run `verify`.
- UI states must distinguish loading, available, unavailable, malformed reply and retry; never imply a booking feature exists.

## Tasks

- [x] Inspect repo/contracts, discover canonical user documents and toolchain; answer local vs GitHub CI status.
- [x] Copy four originals, verify hashes and add labelled extracts/manifest; document PA4 requirements and proposed scope.
- [x] Generate frontend/tooling and Maven Wrapper; record dependency versions. FE tests ran RED before implementation; BE tests were authored first and later verified after toolchain became available.
- [x] Implement minimal health/CSRF/session/security and frontend status boundary; FE 5/5 and BE 8/8 tests pass.
- [x] Wire Dockerfiles/Compose/Nginx, real PostgreSQL migration/integration tests and CI frontend tests; both images build from real source with their checks enabled.
- [x] Run FE lint/typecheck/test/build; backend unit + integration verify; Compose build/up + HTTP smoke.
- [x] Update current-state docs/README/AGENTS/CI setup; read-only review and final report with unverified items.

## Review focus

- Public health must not expose infrastructure detail; protected APIs must return JSON 401, not login pages.
- Valid CSRF without authentication still cannot write; authenticated write without CSRF must fail.
- Session tables must be migrated by Flyway and contain real session data after CSRF bootstrap.
- Frontend must not show a success state on network failure, non-2xx or malformed health response.
- Test DB is separate/temporary, Compose only binds local web port, no real secrets, no remote CI claim.

## Execution note

Docker/portable JDK downloads repeatedly failed with EOF/short read. Backend tests were authored before configuration/controller implementation, but no backend RED run was possible at that stage; do not claim a backend RED→GREEN cycle. The complete clean verify later passed all 5 unit and 3 integration tests on Java 21/PostgreSQL 16, without relaxing commands or baseline. Dockerfile package checks passed in the source build; Compose runtime, proxied HTTP security checks and desktop/mobile ready states also passed. See [verification report](../../test/foundation-verification.md) for evidence and limits. Test services were shut down; the local application was left running on port 8088 for review.
