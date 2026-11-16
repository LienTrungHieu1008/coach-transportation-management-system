# Booking Continuation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development`, as selected by the user, to implement this plan task-by-task. Each implementation task follows RED → GREEN → fresh review. Do not commit, push, open PRs or merge without a separate assignment.

**Goal:** Complete the remaining H1 persistence, H3 hold cleanup and H2 read-only UC-P07 availability flow on the integration branch selected by the user.

**Architecture:** Keep the existing pure Java Booking domain. One concrete `JdbcTemplate` repository reads and updates Booking-owned tables; H2 consumes only the published Trip contract inside one PostgreSQL snapshot. H3 performs expiry in a separate write transaction; viewing availability never creates or cleans up holds.

**Tech Stack:** Existing Java 21, Spring Boot, Spring JDBC, Flyway, Maven Wrapper and PostgreSQL 16; existing JUnit/Spring test support. No new dependency.

**Spec:** The user-approved continuation plan in this conversation, [architecture](../../contracts/architecture.md), [data/API contract](../../contracts/data-and-api.md), [OpenAPI 0.1](../../contracts/openapi.yaml), [TripQueryPort](../../contracts/trip-query-port.md), [C1](../../../pa/PA4/C1_DECISIONS.md), [TASKS](../../../pa/PA4/TASKS.md), [source documents](../../references.md), and the [existing H1 plan](2026-11-15-h1-seat-allocation.md). Read UC 1.8 P07/BR-P07 and SAD 1.3 Booking/Segment/expiry sections through those source documents before implementing; this plan does not amend them.

## Global Constraints

- Preserve the current checkout and all existing H1 work/ignored outputs. Publish the H1 core from `feat/h1-seat-allocation` in a PR targeting `feat/p1-trip-schema`.
- The user superseded the main-only rule on 16/11/2026: use Phuong's `feat/p1-trip-schema` as the integration base. Fast-forward the Huy branch to that base; do not copy/cherry-pick Trip files or alter Trip ownership. A PR into the integration branch does not mean it has merged.
- Keep the Huy branch until its core PR is merged into `feat/p1-trip-schema`. Then switch to that branch and implement subsequent V4/H3/H2 work directly there, as the user requested. Recheck each real schema/port dependency on that integration branch; do not create the previously proposed H3/H2 branches.
- The user authorized the H1 core commit, push and PR into `feat/p1-trip-schema`. This does not authorize merging, deployment, force-pushing or publishing later feature work without assignment. Minh Phuong reviews Booking; the leader still owns migration/contract decisions and final integration.
- No frontend, P08/P09, Trip/Identity internals, shared contract edits, new dependencies, security/CI/build configuration changes or unrelated refactoring. New Booking scheduling settings belong to Booking configuration.
- Migration schema contains no seeds, generated UUID defaults or login credentials. Never edit a shared/applied migration; check V4 ownership/version again before creating it.
- Keep module ownership: runtime Booking SQL joins Booking-owned tables only; use foreign keys for external references and the published port for Trip facts.
- Tests use a dedicated PostgreSQL 16 database, controlled time and no H2, skipped tests, sleeps or fake production port. Do not remove output files.

## Current Integration Workflow — 16/11/2026

The user selected `feat/p1-trip-schema` (PR #4) and a Huy-to-Phuong PR first. The Huy branch has been fast-forwarded to P1 commit `2b671b95111985084bd57880cd86df23e65fc13f`, preserving all 11 existing local files. Publish only the current H1 core/tests and documentation; no V4/H3/H2 implementation is included in that transfer. After this PR is merged, future implementation takes place directly on Phuong's integration branch. Do not merge it autonomously or switch before that gate.

V3 is now available in the chosen base without waiting for main. P3's real adapter, its contract on the integration branch and leader-owned security/error integration remain real dependencies. The leader's confirmation of the layout-ID mapping remains separate from Huy's technical approval.

## Historical Main-only Checkpoint — 16/11/2026

The controller successfully ran `git fetch --prune origin`. `origin/main` is `0760fba871fc4be98210b82f8e208d88cc40ac6c`; its migration tree contains V1 and V2 only, with no production Trip package. PR #4 is open/Draft, `merged_at=null`, head `2b671b95111985084bd57880cd86df23e65fc13f` (V3). PR #3 is open/Draft, `merged_at=null`, head `f4f7e3cc7e398d86c02737efac9aed8dccd2b8c0` (interface/DTO only, no adapter). These are dated observations, not future guarantees.

| Work | Start gate | Checkpoint |
|---|---|---|
| H1 V4, persistence and fixtures | V2 + V3 and their real UUID/schema handoff on `main` | Waiting for V3; existing domain work remains partial H1. |
| H3 | H1/V4 on `main` | Waiting; no feature work started. |
| H2 request/application work | H1 and published Trip contract on `main` | Waiting; no feature work started. |
| H2 runtime acceptance | Real Trip adapter on `main`, same DataSource/transaction participation and leader security/error integration | Waiting; mocks cannot satisfy this gate. |

At that earlier checkpoint, only plan/checkpoint documentation was executable under the then-selected main-only rule. No backend tests ran for that documentation task. The user subsequently replaced that workflow with the integration-branch process above; this historical table is not the current execution gate.

## Review Focus

- Parent Booking, BookingSeat and allocation IDs/Trip/Segment/seat/hold expiry must agree; reject composite mismatches (Task 1).
- Adjacent segments must reuse a seat; no broad unique `(trip_id, physical_seat_id)` that prevents this (Tasks 1–2).
- Cleanup at exact expiry, concurrent workers and partial failure must preserve transaction atomicity and unrelated records (Task 3).
- Missing, duplicate or malformed UUIDs and failed/unverified reads must return contract errors with `no-store`, never a FREE fallback or PII (Task 4).
- A concurrent Trip/layout/allocation change must not mix snapshots; real-port tests use two connections and barriers (Task 5).

## File Map

Future implementation paths below are relative to the repository root. Preserve and reuse existing domain files rather than adding parallel abstractions.

- Existing core: `src/backend/src/main/java/vn/edu/team11/coach/booking/domain/{BookingStatus,Segment,SeatAllocation,SeatAllocationService}.java`; existing tests under the matching `src/backend/src/test/java/.../booking/domain/` directory.
- H1: create `src/backend/src/main/resources/db/migration/V4__booking_availability.sql`; `src/backend/src/main/java/vn/edu/team11/coach/booking/infrastructure/BookingRepository.java`; matching `BookingSchemaIT.java` and `BookingRepositoryIT.java` under `src/backend/src/test/java/vn/edu/team11/coach/booking/infrastructure/`; `src/backend/src/test/resources/fixtures/booking_availability.sql`. Extend the seed instructions in `docs/development.md`; keep seed/reset SQL separate from migrations and use the real Account/Trip fixture handoff.
- H3: create `src/backend/src/main/java/vn/edu/team11/coach/booking/application/BookingExpiryService.java`, `.../booking/infrastructure/BookingConfiguration.java` and `BookingExpiryScheduler.java`; tests at matching `.../booking/application/BookingExpiryServiceTests.java`, `.../booking/infrastructure/BookingExpirySchedulerTests.java` and `BookingExpiryIT.java`. Extend `src/backend/src/main/resources/application.yaml` only for the approved Booking cleanup environment settings.
- H2: create `src/backend/src/main/java/vn/edu/team11/coach/booking/application/SeatAvailabilityApplicationService.java`, `.../booking/api/SeatAvailabilityController.java`, `SeatAvailabilityResponse.java` and module-local `SeatAvailabilityExceptionHandler.java`; matching application/API tests and `src/backend/src/test/java/vn/edu/team11/coach/booking/infrastructure/SeatAvailabilityIT.java`. Reuse published Trip DTO/error names after checking their merged declarations; do not invent a replacement contract or production stub.
- Evidence: update `docs/test/h1-seat-allocation-verification.md`; add `docs/test/h3-hold-expiry-verification.md` and `docs/test/h2-seat-availability-verification.md` when those tasks actually run; update `pa/PA4/TASKS.md`. Supply Booking content for QA's PA4 SAD revision; preserve the PA3 PDF.

## Task 0: Preserve Work and Record Gates

**Files:** This plan, `docs/test/h1-seat-allocation-verification.md`, `pa/PA4/TASKS.md` only.

**Interfaces:** Consumes the approved plan and fetched main/PR evidence; produces a dependency checkpoint, without implementing a feature.

- [x] Read repo rules/contracts/handbook, inspect `git status`, and retain the current branch and all local work.
- [x] Save the approved decisions and dated dependency gates here; append checkpoints without rewriting historical test records.
- [x] Check local Markdown links, whitespace and unchanged source hashes against `/tmp/team11-booking-continuation-jev7bijz/source-hashes.json`; obtain fresh documentation review and report actual checks. Passed: 25 local links, seven unchanged source/test hashes, whitespace/newlines; fresh reviewer found no spec or quality issues. No backend tests rerun.

## Task 1: H1 V4 Integrity — After Core PR Integration

**Files:** V4 migration, `BookingSchemaIT.java`, Booking fixture and existing H1 verification report from the file map.

**Interfaces:** Consumes V2 `identity_account(account_id)` and the merged V3 schema/Account–Trip–Stop–six-seat UUID manifest. Produces `booking`, `booking_seat`, `seat_allocation`; no Booking creation/payment API.

- [ ] After the H1 core PR is merged, switch to the updated `feat/p1-trip-schema`, verify V3/manifest there, inspect actual keys/constraints and confirm V4 is unused. Record the full backend baseline before implementation. Preserve the owner's Trip files.
- [ ] RED: add schema IT cases for fresh V1→V4 and V3→V4 upgrade preserving Account/session/Trip rows; invalid FK/composite parent/seat/Trip/Segment/expiry, invalid status/kind, negative amounts, blank traveller and adjacent segment seat reuse. Run `BookingSchemaIT` and observe the expected missing-V4/schema failure.
- [ ] GREEN: add V4 only, with UUIDs supplied by callers/fixtures and no seed/default UUID generation. `booking` has passenger Account FK, Trip FK, `(trip_id, from_stop_id)` and `(trip_id, to_stop_id)` FKs to `trip_stop(trip_id, stop_id)`, valid `0 <= from_stop_index < to_stop_index`, statuses `Pending/Confirmed/Expired/Cancelled`, nonnegative BIGINT total VND, and hold creation/expiry exactly 10 minutes apart.
- [ ] GREEN: `booking_seat` has UUID PK, Booking FK, physical-seat FK, nonblank traveller name, nonnegative BIGINT fare VND, unique Booking/seat. `seat_allocation` has UUID PK, BookingSeat FK, additional database-only `booking_id` for integrity, Trip, physical seat, Segment indices, `Hold/Confirmed`, expiry and `released_at`. Composite FKs ensure allocation parent Booking/BookingSeat/seat/Trip/Segment agree; Hold expiry is required and matches its Booking, while Confirmed expiry may be null. Keep the extra `booking_id` out of the domain/API. Add candidate-read and Pending-expiry indexes, with no unique Trip/seat restriction.
- [ ] Run `BookingSchemaIT` GREEN, inspect the constraints against the merged V3 schema, then obtain fresh review of migration integrity and unchanged existing migration files. Fixture assertions cover 1–6 distinct seats and Booking total equal to seat fares; this does not claim an implemented P08 invariant/API.

## Task 2: H1 Repository and Reproducible Fixtures — Pending Task 1

**Files:** `BookingRepository.java`, `BookingRepositoryIT.java`, Booking fixture, seed instructions and H1 evidence.

**Interfaces:** One concrete `JdbcTemplate` repository exposes `List<SeatAllocation> findCandidates(UUID tripId, Segment segment)` and `int expireDueBookings(Instant asOf)`. Task 2 implements the candidate read; Task 3 implements expiry. Do not expose a placeholder expiry method before its implementation. Reuse existing `SeatAllocationService.getAvailability(UUID, Segment, List<UUID>, List<SeatAllocation>, Instant)` and domain types.

- [ ] RED: add candidate IT cases asserting correct Booking join/status, overlapping candidates, adjacent exclusion and propagation of database/read errors. Run `BookingRepositoryIT` and observe failure for the absent repository/read.
- [ ] GREEN: read candidates using only Booking-owned joins; map the parent Booking status and allocation fields into the current domain. Preserve core effectiveness/overlap logic; never catch a failed read and return an empty list.
- [ ] Add fixture tests using the shared Account/Trip/Stop/six-seat UUID manifest: FREE; Confirmed A–B; Confirmed A–C; live Hold B–C; expired Hold; cancelled/released allocation. Assert both A–C and B–C results with fixed clocks, 1–6 distinct seats and matching totals. Keep all-seats-unavailable and incomplete-data scenarios separate.
- [ ] Use one `seedTime` in runtime demo seed: live hold created `seedTime−8m`, expires `seedTime+2m`. Seed Account → Trip → Booking atomically using the owners' actual fixture inputs, never recreate Trip/Identity data in Booking. Reset only an explicitly isolated local/test DB; no arbitrary shared DB cleanup.
- [ ] Run repository/schema/domain tests GREEN and full PostgreSQL verify; record exact counts/results and reproducible seed/input/expected. Obtain fresh H1 review, Minh Phương's member review and leader migration/contract review. Keep H1 partial until its real acceptance criteria and merge are satisfied; no autonomous Git publication.

## Task 3: H3 Atomic Hold Expiry — Pending V4 on the Integration Branch

**Files:** H3 files and existing repository/configuration paths from the file map; H3 evidence and TASKS.

**Interfaces:** `BookingExpiryService.expireDueBookings()` returns the count from `BookingRepository.expireDueBookings(Instant asOf)`. Inject the named `bookingClock` (`Clock.systemUTC()` in production); take one `Instant`, truncated to microseconds, per call.

- [ ] Verify H1/V4 is present and tested on `feat/p1-trip-schema`; keep working directly on that branch. Run the dependency-ready baseline.
- [ ] RED: fixed-clock tests for before/at/after expiry, multiple seats, two idempotent runs, two concurrent workers, Confirmed/Cancelled/already-released/history untouched, and rollback after a controlled partial failure. Use synchronization barriers, not sleeps. Run `BookingExpiryServiceTests,BookingExpiryIT` and observe the missing-expiry behavior fail.
- [ ] GREEN: service owns the write transaction. Select due Pending rows with `expires_at <= asOf`, `ORDER BY booking_id FOR UPDATE SKIP LOCKED`; conditionally set only those Bookings to Expired, then release only their unreleased Hold allocations. Return the changed Booking count. Do not release Confirmed, delete history, extend expiry or modify other statuses.
- [ ] RED/GREEN: a separate scheduled bean calls the transactional service (no self-invocation, GET writes or duplicated cleanup). Test scheduling disabled/enabled and invalid intervals. Bind `BOOKING_EXPIRY_CLEANUP_ENABLED` default `true`; `BOOKING_EXPIRY_CLEANUP_INTERVAL` default `PT5S`, strictly positive and no greater than five seconds; schedule at fixed rate. Reject invalid configuration.
- [ ] Disable the automatic job in tests; call the service directly with a fixed `bookingClock`. Run H3 tests GREEN and full PostgreSQL verify, obtain fresh review, and record actual results/SAD content. H3 remains pending member review/merge where applicable; do not publish without assignment.

## Task 4: H2 Contract Behavior — Pending H1 and Public Trip Contract

**Files:** H2 application/controller/response/error-handler tests from the file map; H2 evidence and TASKS.

**Interfaces:** `GET /api/v1/trips/{tripId}/seat-availability?fromStopId&toStopId`; application `getSeatAvailability(UUID tripId, UUID fromStopId, UUID toStopId)` returns `SeatAvailabilityResponse`. Consume published `TripQueryPort.getSeatAvailabilityFacts(UUID, UUID, UUID, Instant)`, H1 candidate read and existing domain calculation. Use exact merged DTO/error declarations and exact OpenAPI response fields.

- [ ] Verify H1 and the owner-published Trip interface/DTO/errors are present on `feat/p1-trip-schema`; continue there after the H3 task sequence. If the real adapter is absent, limit claims to targeted unit/MVC behavior; do not register a fake production bean or claim full runtime acceptance.
- [ ] RED: add unit/MVC cases for missing/duplicate/noncanonical UUIDs → 400; all typed errors; failed or mismatched facts/read → 503 without FREE fallback; no PII; complete ordered seat/layout mapping; all blocked → 200; Confirmed expiry null; Hold expiry minimum; expired Hold ignored while cleanup is late. Assert `Cache-Control: no-store` on success and error responses. Run `SeatAvailabilityApplicationServiceTests,SeatAvailabilityControllerTests` and observe missing behavior fail.
- [ ] GREEN: controller validates canonical UUID syntax and duplicate scalars before the port. Application uses read-only REQUIRED, REPEATABLE_READ on the shared transaction manager; captures one microsecond-truncated `asOf` from `bookingClock`; calls the port, checks fact IDs/asOf and verified data, reads candidates and delegates to the existing core. Map exact OpenAPI `tripId/fromStopId/toStopId/serverTime/bookingClosesAt/layout/seats` fields with all seats in layout order; the port's `seatLayout` maps to HTTP `layout`. Return only FREE/UNAVAILABLE on 200; create no holds and perform no cleanup.
- [ ] Map typed failures: Trip missing → 404; Stop/Segment → 422; not bookable/essential incomplete → 409; failed/unverified reads → 503 `AVAILABILITY_UNAVAILABLE`; unexpected errors → 500 `INTERNAL_ERROR`. Use the contract error body and no-store throughout; never include SQL, stack traces, Booking IDs or traveller data.
- [ ] Run targeted tests GREEN and obtain fresh review. Mock the published port only in unit/MVC tests. Hand exact GET path/error requirements to the leader for common security/error integration; do not edit shared security or allowlist `/api/**`. Keep real-port/snapshot/public-runtime acceptance pending Task 5.

## Task 5: H2 Real-Port Acceptance and Handoff — Pending Adapter/Security

**Files:** `SeatAvailabilityIT.java`, actual H1/H2/H3 reports and TASKS; Booking content supplied for the PA4 SAD revision.

**Interfaces:** Real Trip adapter joins the caller's REQUIRED transaction on the same DataSource/transaction manager. Leader owns exact public GET and common errors; all other protected routes remain protected.

- [ ] Verify the real adapter and leader integration are present on the tested integration branch; inspect same-DataSource REQUIRED participation with no REQUIRES_NEW/provider call. Run the dependency-ready baseline; no real adapter means no complete H2 acceptance.
- [ ] RED: add PostgreSQL snapshot IT using two DB connections and barriers to change Trip/layout/allocations between reads. Assert one consistent snapshot and one asOf. Add real port error/cutoff/Segment, ordered full map, all-blocked 200, delayed-cleanup expiry and no-store/no-PII cases; assert the exact public GET is accessible and other protected endpoints stay protected.
- [ ] GREEN: resolve only Booking-owned defects; route Trip/security/common-error defects to the owner. Run all relevant targeted tests and the full backend verification below. Do not mask an unavailable/failed read, weaken gates or treat mocked integration as real-port evidence.
- [ ] Record actual commands, environment/build, migrations, seed/input/expected/actual and outstanding limits in reports. Execute at least five real UC-P07 functional scenarios for QA; domain cases do not equal the PA4 fifteen-case report. Supply Booking flow, data changes, transaction, error branch and implementation facts for the separate PA4 SAD revision; do not edit the submitted PA3 PDF.
- [ ] Obtain fresh whole-change review plus Minh Phương/leader reviews required by the repo; mark only verified scope complete. No commit/push/PR/merge unless explicitly assigned.

## Verification Commands and Reporting

From the repository root, once the task's dependencies are ready:

```bash
docker compose -p team11-huy-booking-test -f compose.test.yaml run --rm backend-test
docker compose -p team11-huy-booking-test -f compose.test.yaml down
```

This runs the existing Java 21/PostgreSQL 16 `clean verify -DfailIfNoTests=true` workflow. Down without `-v`; retain ignored outputs. Inspect actual Surefire/Failsafe results and report counts/failures/errors/skips, without forecasting a total or equating existing foundation/Account IT with Booking acceptance.

Use the same isolated Compose project for targeted RED/GREEN runs, passing the relevant named test classes:

```bash
docker compose -p team11-huy-booking-test -f compose.test.yaml run --rm backend-test \
  sh ./mvnw -B -ntp -Dtest=SeatAvailabilityApplicationServiceTests,SeatAvailabilityControllerTests \
  test -DfailIfNoTests=true
```

For integration targets use `-Dit.test=BookingSchemaIT` (or the relevant IT classes) with `verify -DfailIfNoTests=true`; unit classes use `-Dtest=... test`. Never use skip flags. Record the exact command and expected RED reason before implementation, then actual GREEN results. Run frontend checks only if frontend changes are separately authorized and made. Documentation-only checkpoints verify links, whitespace, claims and preservation; they do not rerun backend tests or alter historical results.
