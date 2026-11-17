
# P1 Trip Demo Manifest — Team 11

Owner: Do Minh Phuong
Reviewer: Truong Dinh Nhat Huy
Status (16/11/2026): P1/V3 merged via PR #4; layout mapping confirmed by leader in `docs/contracts/trip-query-port.md`. P3 PostgreSQL adapter is pending review/merge in PR #6. Merge status does not establish V3/V4 integration acceptance or approval of later changes.

## 1. Source

Migration:
`src/backend/src/main/resources/db/migration/V3__trip_read_model.sql`

Fixture:
`src/backend/src/test/resources/fixtures/trip_demo.sql`

Runtime demo seed:
`src/backend/src/test/resources/fixtures/trip_demo_runtime.sql`

Runtime seed/reset guide:
`docs/test/p1-trip-runtime-seed-guide.md`

Integration tests:
`src/backend/src/test/java/vn/edu/team11/coach/trip/infrastructure/TripSchemaIT.java`

Database: PostgreSQL 16

## 2. Route and Stops

Route ID:
`44444444-4444-4444-8444-444444444444`

Route code: `DEMO-ABC`

| Stop | UUID | stop_order |
|---|---|---|
| A | 22222222-2222-4222-8222-222222222222 | 0 |
| B | 33333333-3333-4333-8333-333333333333 | 1 |
| C | 99999999-9999-4999-8999-999999999999 | 2 |

Stop indices are 0-based and come from
`route_stop.stop_order`.

Segment interval: `[fromStopIndex, toStopIndex)`.

Examples:

- A -> B: [0,1)
- B -> C: [1,2)
- A -> C: [0,2)

## 3. Vehicle and Physical Seats

Vehicle ID:
`66666666-6666-4666-8666-666666666666`

Vehicle code: `DEMO-VEHICLE-01`
License plate: `47B-123.45` (synthetic demo value)

Vehicle type: `Demo coach`

Layout template: `SEATED_6_1_DECK`

Seat layout ID:
`bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb`

Layout identity mapping (confirmed by leader, 16/11/2026):

- `Vehicle.seatLayoutId` = `vehicle.seat_layout_id`.
- `SeatLayout.id` = `vehicle.seat_layout_id`.
- Each PhysicalSeat keeps its own stable `physical_seat_id`.
- Do not generate new layout/seat UUIDs when reading the database.
- Current V3 uses one distinct layout ID per Vehicle.
- No separate `seat_layout` table exists in V3; construct the DTO from Vehicle and its PhysicalSeats.

Capacity: 6

| Label | physicalSeatId | deck | seat_row | seat_column |
|---|---|---|---|---|
| A1 | 77777777-7777-4777-8777-777777777777 | 0 | 0 | 0 |
| A2 | 88888888-8888-4888-8888-888888888888 | 0 | 0 | 2 |
| B1 | 77777777-7777-4777-8777-777777777778 | 0 | 1 | 0 |
| B2 | 77777777-7777-4777-8777-777777777779 | 0 | 1 | 2 |
| C1 | 77777777-7777-4777-8777-777777777780 | 0 | 2 | 0 |
| C2 | 77777777-7777-4777-8777-777777777781 | 0 | 2 | 2 |

Seat UUIDs are stable identifiers.

Never derive physicalSeatId from seat label or coordinates.

Coordinates are 0-based.

## 4. Schedules and Trips

| Trip | UUID | Schedule UUID | Departure VN |
|---|---|---|---|
| Day | 11111111-1111-4111-8111-111111111111 | aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa10 | 20/11/2026 09:00 |
| Overnight | 11111111-1111-4111-8111-111111111112 | aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa11 | 20/11/2026 22:30 |

### Schedule representation

- `schedule.operating_days` is stored as TEXT.
- The supported demo value is `DAILY`, meaning every day.
- Weekly operating-day patterns are not implemented in P1.
- `first_stop_departure_time` uses local time in
  `Asia/Ho_Chi_Minh`.
- Actual TripStop instants are stored as TIMESTAMPTZ.
- Trip queries must use the stored TripStop instants rather
  than reconstructing them from the Schedule template.
Both Trips:

- Route: DEMO-ABC
- Vehicle: DEMO-VEHICLE-01
- Status: Scheduled
- Departure date: 2026-11-20

The fixture uses fixed dates for repeatable tests.


A separate runtime demo seed uses D = tomorrow in
Asia/Ho_Chi_Minh. See `trip_demo_runtime.sql` and
`docs/test/p1-trip-runtime-seed-guide.md`.

The fixed fixture remains unchanged for repeatable tests.

## 5. Trip Stop Times

All UTC instants below refer to the fixed test date.

| Trip | Stop | UTC |
|---|---|---|
| Day | A departure | 2026-11-20T02:00:00Z |
| Day | B arrival/departure | 2026-11-20T04:00:00Z |
| Day | C arrival | 2026-11-20T06:00:00Z |
| Overnight | A departure | 2026-11-20T15:30:00Z |
| Overnight | B arrival/departure | 2026-11-20T17:30:00Z |
| Overnight | C arrival | 2026-11-20T19:30:00Z |

## 6. Booking Cutoff

C1 rule:

bookingClosesAt = first Stop departure - 30 minutes

| Trip | bookingClosesAt UTC |
|---|---|
| Day | 2026-11-20T01:30:00Z |
| Overnight | 2026-11-20T15:00:00Z |

Trip is closed when `asOf >= bookingClosesAt`.

Cutoff does not change for intermediate Stops.

## 7. Leg Fares

| Segment | VND |
|---|---:|
| A -> B | 100000 |
| B -> C | 150000 |
| A -> C | 250000 |

A -> C is calculated as the sum of A -> B and B -> C.

There is no separate A -> C leg in the fixture.

Both demo Trips have identical leg fares.

## 8. Verification

Command:

`docker compose -f compose.test.yaml run --rm backend-test`

Latest reported result on 2026-11-16:

- Tests run: 62
- Failures: 0
- Errors: 0
- Skipped: 0
- BUILD SUCCESS
- Exit code: 0

Cleanup:

`docker compose -f compose.test.yaml down`

This reports the latest Maven results from the P1 working branch.
It does not certify all P3/P07 runtime behavior.

## 9. Review Status and Remaining Work

### Confirmed by Huy for V4

- Stop index is 0-based.
- Segment interval is `[fromIndex, toIndex)`.
- Existing six PhysicalSeat UUIDs and two Trip UUIDs
  are sufficient for V4 Booking/hold tests.
- `DAILY` is acceptable for the fixed demo fixture.
- PhysicalSeat UUIDs must remain stable.

### Implemented locally, pending PR update

- Vehicle `license_plate` with a unique constraint.
- Vehicle `layout_template`.
- Stable Vehicle `seat_layout_id` (mapping confirmed by leader on 16/11/2026).
- Integration tests for Vehicle fields and constraints.
- Fixture checks for contiguous RouteStop indices.
- Fixture checks for chronological TripStop times.
- Fixture checks for complete, forward and consecutive LegFares.
- Latest verification: 62 tests, BUILD SUCCESS.



### Remaining work

- Layout mapping confirmation is complete; use the published port contract.
- Implement runtime validation for Stop index continuity,
  chronological TripStop times, and forward/consecutive
  LegFares with complete fare coverage in the P3 DB adapter.
- Handle incomplete or inconsistent Trip facts according to
  the existing TripQueryPort/API contract; never default
  missing fares to zero.
- Implement the P3 PostgreSQL TripQueryPort adapter.
- Complete final V3/V4 integration review before merge.


No Booking or SeatAllocation records are created by
this fixture.
