
# P1 Trip Demo Manifest — Team 11

Owner: Do Minh Phuong
Reviewer: Truong Dinh Nhat Huy
Status: Draft — pending review

## 1. Source

Migration:
`src/backend/src/main/resources/db/migration/V3__trip_read_model.sql`

Fixture:
`src/backend/src/test/resources/fixtures/trip_demo.sql`

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

Both Trips:

- Route: DEMO-ABC
- Vehicle: DEMO-VEHICLE-01
- Status: Scheduled
- Departure date: 2026-11-20

The fixture uses fixed dates for repeatable tests.

Runtime demo with D = tomorrow is not yet implemented.

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

- Tests run: 52
- Failures: 0
- Errors: 0
- Skipped: 0
- BUILD SUCCESS
- Exit code: 0

Cleanup:

`docker compose -f compose.test.yaml down`

This reports the latest Maven results from the P1 working branch.
It does not certify all P3/P07 runtime behavior.

## 9. Pending Review

- Confirm PhysicalSeat coordinates and Stop index with Huy.
- Confirm operating_days format.
- Review Schedule/Trip relationships and layout identifier.
- Validate Stop order continuity and consecutive LegFare coverage.
- Complete missing/invalid Trip data tests.
- Prepare runtime demo dates using D = tomorrow.
- Review V3 before integration with V4.

No Booking or SeatAllocation records are created by this fixture.
