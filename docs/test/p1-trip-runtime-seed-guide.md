
# P1 Trip Runtime Demo — Seed and Reset Guide

Owner: Do Minh Phuong
Scope: Local, isolated demo database only.

## Purpose

The runtime seed creates the same Route, Stops, Vehicle,
PhysicalSeats, Schedules, Trips and LegFares as the fixed
P1 fixture, but uses D = tomorrow in Asia/Ho_Chi_Minh.

- Day Trip: D 09:00, 11:00, 13:00.
- Overnight Trip: D 22:30, D+1 00:30, 02:30.
- Stable IDs are shared with Huy's V4 demo/tests.
- Booking cutoff remains 30 minutes before the first Stop.
- Prices: A-B 100000 VND, B-C 150000 VND.

## Files

Fixed test fixture:
`src/backend/src/test/resources/fixtures/trip_demo.sql`

Runtime seed:
`src/backend/src/test/resources/fixtures/trip_demo_runtime.sql`

Manifest:
`docs/test/p1-trip-demo-manifest.md`

## Preconditions

1. Use an isolated, disposable PostgreSQL demo database.
2. Apply Flyway V1, V2 and the matching V3 schema in order.
3. Verify that the demo UUIDs do not already exist.
4. Do not run this seed on a shared database, production
   database, or a database containing V4 Booking data.
5. Confirm that the target database is the intended demo DB.

## Seeding

Execute `trip_demo_runtime.sql` using a PostgreSQL SQL
client connected to the isolated demo database.

Execute only once against a clean database.

The file is not a Flyway migration and must not be added
to `db/migration`.

## Verification

After seeding, verify:

- Exactly one demo Route with three ordered Stops.
- One Vehicle with six stable PhysicalSeats.
- Two Scheduled Trips departing on D.
- Both Trips have two consecutive LegFares.
- TripStop instants preserve local Vietnam time and
  overnight rollover.

Example read-only verification query:

```sql
SELECT
    trip_id,
    departure_date,
    status
FROM trip
WHERE trip_id IN (
    '11111111-1111-4111-8111-111111111111',
    '11111111-1111-4111-8111-111111111112'
)
ORDER BY trip_id;
```

Expected `departure_date`:
tomorrow's date in `Asia/Ho_Chi_Minh`
at seed execution time.

## Reset

Do not delete Booking, BookingSeat or SeatAllocation
records to re-run this seed.

For reset, dispose of the entire isolated demo database
and recreate it through the standard Flyway migration
process, then execute the runtime seed once again.

Never use `TRUNCATE ... CASCADE`, `DROP SCHEMA` or
`docker compose down -v` against a shared environment.

## Limitations

- This seed is not idempotent: repeated execution using
  the same UUIDs will cause primary-key conflicts.
- Runtime dates are evaluated when the SQL runs.
  An existing seeded Trip does not move to tomorrow
  automatically on subsequent days.
- The V4 Booking fixture and hold/expiry behavior belong
  to Huy.
- P3 runtime validation and API implementation are
  separate deliverables.

## Run the runtime seed atomically

Run only against a fresh, isolated demo PostgreSQL database
after Flyway migrations have completed.

Use `psql` with both flags:

- `-1` (`--single-transaction`): Execute the entire seed file in one transaction.
- `-v ON_ERROR_STOP=1`: Stop immediately when a SQL or script error occurs.
- `-X`: Ignore local psql startup configuration.

From the repository root, run this command in Windows CMD:

```bat
psql -X -v ON_ERROR_STOP=1 -1 -d "<DEMO_DATABASE_URL>" -f "src/backend/src/test/resources/fixtures/trip_demo_runtime.sql"
```

Replace `<DEMO_DATABASE_URL>` with the PostgreSQL connection
string for your isolated demo database.

If any statement fails, psql stops with a nonzero exit code,
and the entire seed transaction is rolled back.

Do not run this seed against shared or production databases.
The seed uses fixed UUIDs and is not idempotent. To reseed,
recreate the isolated demo database first.

Do not add BEGIN or COMMIT to the SQL fixture because
`psql -1` manages the transaction.
