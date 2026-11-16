# H1 Seat Allocation Core Implementation Plan

**Goal:** Implement the independent H1 domain rules and unit tests for UC-P07.

**Architecture:** Pure Java records, enums and one domain service in `booking.domain`. Caller supplies verified Trip/seat IDs and a single authoritative `Instant asOf`; no Spring, JPA, clock or provider dependency in the domain.

**Spec:** User-approved plan, UC 1.8 BR-P07-01..04 / TC-13..14, SAD 1.3 section 4.4 C1..C3/D6, C1, OpenAPI 0.1 and `docs/contracts/trip-query-port.md`.

## Constraints

- Work in the current checkout on `feat/h1-seat-allocation`, based on updated main.
- Do not copy PR #3 or import Trip internals. Do not change shared contracts, dependencies, security, CI, migrations or frontend.
- Pending-to-Expired transitions and the H3 scheduler are deferred by the user. V4/persistence/API require the remaining Trip handoff.
- No commit, push, PR or merge in this task. Test artifacts remain ignored.
- Use English exception messages; no production logs, invented configuration or placeholder layers.

## Review Focus

- Half-open boundaries and identical/nested Segments.
- Booking/kind/released combinations and exact expiry with caller-provided time.
- Confirmed blockers override hold refresh hints; otherwise use the earliest blocking hold expiry.
- Caller seat order, complete immutable results and unchanged input.
- Invalid input must fail rather than imply FREE.

## Tasks

- [x] Update main, create branch and verify the baseline using the dedicated PostgreSQL test project.
- [x] Write `SegmentTests`, observe RED, implement `Segment(fromStopIndex, toStopIndex)` and `overlaps`, then verify GREEN.
- [x] Write `SeatAllocationTests`, observe RED, implement `BookingStatus` and immutable `SeatAllocation` with `Kind` and `isEffective(asOf)`, then verify GREEN.
- [x] Write `SeatAllocationServiceTests`, observe RED, implement `getAvailability(UUID, Segment, List<UUID>, List<SeatAllocation>, Instant)` and nested result/status types, then verify GREEN.
- [x] Run full backend verify, review the complete change and record actual verification plus remaining dependencies. Update H1 to partial completion only.

## Domain behavior

- Segment requires `0 <= fromStopIndex < toStopIndex`; overlap is `from < other.to && other.from < to`.
- SeatAllocation carries tripId, physicalSeatId, Segment, BookingStatus, Kind, expiresAt and releasedAt. Hold requires expiry; Confirmed permits null expiry. It is read-side domain input, not the V4 persistence mapping.
- Unreleased Confirmed/Confirmed blocks; unreleased Pending/Hold blocks only before expiresAt. Other status/kind combinations do not block.
- Service validates required inputs and distinct nonempty seat IDs, groups relevant effective allocations by physical seat and returns immutable SeatState values in caller order.
- No blockers yields FREE/null; any Confirmed blocker yields UNAVAILABLE/null; only Hold blockers yield UNAVAILABLE/earliest expiry. No unknown/free fallback on invalid input.

## Verification

Targeted tests, from `src/backend`:

```bash
./mvnw -B -ntp -Dtest=SegmentTests,SeatAllocationTests,SeatAllocationServiceTests test -DfailIfNoTests=true
```

Full baseline/final verification, from the repository root:

```bash
docker compose -p team11-huy-h1-test -f compose.test.yaml run --rm backend-test
docker compose -p team11-huy-h1-test -f compose.test.yaml down
```

Use a dedicated PostgreSQL 16 test database, no H2 or skipped tests. No frontend changes require frontend checks. The handoff must distinguish H1 domain tests from V4 integration, H2 runtime and H3 cleanup.
