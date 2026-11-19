# P1/P2/P4 runtime QA scenario pack

Audience: Hoang (frontend), Duc Hieu (QA). Scope: UC-P04 search and UC-P05 details.
Prepared 18/11/2026. All HTTP outcomes below are **predicted assertions**, not
executed evidence. Initial preparation received static review only. PostgreSQL
execution results subsequently supplied from the user's terminal are recorded
below; no runtime commands were executed by the agent for this evidence update.

## Implementation and source boundary

- P2 is on main, inspected at `0115a8c`: `TripSearchPolicy`,
  `PostgresTripSearchRepository`, `TripSearchService`, and their existing tests.
- **P4 is not merged into main.** Details assertions require an app built with
  PR #13, `feat/p4-trip-details`, inspected at `fb28ece`. Its policy, repository,
  response mapper, repository/domain tests, and handoff were read through Git.
  This guide does not instruct a branch switch. Record the actual app commit.
- Main permits anonymous GET search; its details path is still protected and has
  no P4 implementation. An anonymous 401 on main does not exercise P4. The
  inspected P4 branch includes the anonymous GET details security matcher.
- Sources: [V3](../../src/backend/src/main/resources/db/migration/V3__trip_read_model.sql),
  [runtime seed](../../src/backend/src/test/resources/fixtures/trip_demo_runtime.sql),
  [seed/reset guide](p1-trip-runtime-seed-guide.md),
  [manifest](p1-trip-demo-manifest.md), [OpenAPI](../contracts/openapi.yaml),
  [design decisions](../contracts/p2-p4-design-decisions.md),
  [P2 handoff](p2-b-trip-search-handoff.md), [TripQueryPort](../contracts/trip-query-port.md).
  P4 handoff: `git show feat/p4-trip-details:docs/test/p4-trip-details-handoff.md`.
  Historical premerge notes in handoffs are not proof of current runtime results.

## Isolated baseline and exact identities

Use a confirmed isolated disposable PostgreSQL 16 database, with the matching
Flyway schema and **only the unchanged P1 runtime seed in the nine Trip tables**.
V4 schema may exist, but there must be no Booking/BookingSeat/SeatAllocation data.
Do not load the Booking runtime fixture. No other users or background writers may
change this database during the exercise. Verify host, port, database, app datasource
and isolation yourself; the explicit confirmation variable is an acknowledgement,
not a technical proof that the database is disposable.

Follow the existing seed guide when preparing a fresh environment later; the pack
does not create a database or seed it. Never rerun the non-idempotent seed over
existing UUIDs. No fixture, migration, application configuration or stable ID changes
are required. Scripts assume the matching V3 tables are in `public`.

| Object | Exact UUID |
|---|---|
| Day Trip | `11111111-1111-4111-8111-111111111111` |
| Overnight Trip | `11111111-1111-4111-8111-111111111112` |
| Route DEMO-ABC | `44444444-4444-4444-8444-444444444444` |
| Stop A, order 0 | `22222222-2222-4222-8222-222222222222` |
| Stop B, order 1 | `33333333-3333-4333-8333-333333333333` |
| Stop C, order 2 | `99999999-9999-4999-8999-999999999999` |
| Shared Vehicle, capacity 6 | `66666666-6666-4666-8666-666666666666` |
| Layout | `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb` |
| Day Schedule | `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa10` |
| Overnight Schedule | `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa11` |
| A1, position (0,0,0) | `77777777-7777-4777-8777-777777777777` |
| A2, position (0,0,2) | `88888888-8888-4888-8888-888888888888` |
| B1, position (0,1,0) | `77777777-7777-4777-8777-777777777778` |
| B2, position (0,1,2) | `77777777-7777-4777-8777-777777777779` |
| C1, position (0,2,0) | `77777777-7777-4777-8777-777777777780` |
| C2, position (0,2,2) | `77777777-7777-4777-8777-777777777781` |

Positions are `(deck, seat_row, seat_column)`. Both Trips start Scheduled, share
the Vehicle and have A-B=100000 and B-C=150000 VND. A-C=250000 is calculated;
there is no stored A-C fare row. All Route/Stop/Vehicle active flags remain true.

**D means the recorded seed departure date**, tomorrow in Asia/Ho_Chi_Minh at
seed execution. It does not advance on subsequent days. Do not use the fixed
fixture's 20/11/2026 date as a runtime assumption.

| Trip | A departure | B arrival/departure | C arrival | Cutoff |
|---|---|---|---|---|
| Day | D 09:00 VN | D 11:00 VN | D 13:00 VN | D 08:30 VN |
| Overnight | D 22:30 VN | D+1 00:30 VN | D+1 02:30 VN | D 22:00 VN |

These instants correspond to UTC D 02:00/04:00/06:00 and D 15:30/17:30/19:30.
Arrival at A and departure at C are null by design. Before any scenario, record
the stored values with this read-only query (using the confirmed connection):

```sql
SELECT t.trip_id, t.departure_date, t.status, rs.stop_order, ts.stop_id,
       ts.planned_arrival_time, ts.planned_departure_time,
       ts.planned_departure_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS departure_vn,
       CASE WHEN rs.stop_order = 0
            THEN ts.planned_departure_time - INTERVAL '30 minutes' END AS cutoff
FROM public.trip t
JOIN public.trip_stop ts ON ts.trip_id = t.trip_id AND ts.route_id = t.route_id
JOIN public.route_stop rs ON rs.route_id = ts.route_id AND rs.stop_id = ts.stop_id
WHERE t.trip_id IN ('11111111-1111-4111-8111-111111111111',
                    '11111111-1111-4111-8111-111111111112')
ORDER BY t.trip_id, rs.stop_order;
```

## Commands and restoration protocol

All commands are templates for a later authorized runtime session, from the repo
root in PowerShell. Replace placeholders; do not store credentials in tracked files
or evidence. Set D from the query above, not from today's date. Use a fresh `psql`
process for each operation, always with `-X -v ON_ERROR_STOP=1 -1`.

```powershell
$qaConnection = '<confirmed isolated demo connection string>'
$qaDatabase = '<exact current_database() value on that connection>'
$qaSeedDate = '<recorded D in YYYY-MM-DD>'
$qaDirectory = 'docs/test/sql/p1-p4-runtime-qa'
$qaArguments = @('-X', '-v', 'ON_ERROR_STOP=1', '-1', '-d', $qaConnection,
    '-v', "qa_database=$qaDatabase", '-v', "qa_seed_date=$qaSeedDate",
    '-v', 'qa_isolated=CONFIRMED_ISOLATED_DISPOSABLE')

function Invoke-QaScenarioSql {
    param([string]$File, [string]$Status)
    $qaRunArguments = $qaArguments + @('-f', "$qaDirectory/$File")
    if ($Status) { $qaRunArguments += @('-v', "qa_status=$Status") }
    & psql @qaRunArguments
    if ($LASTEXITCODE -ne 0) { throw "QA SQL failed: $File; stop and inspect" }
}

# Baseline check: no persistent mutation; also use after EVERY restoration.
& psql @qaArguments -f "$qaDirectory/_guard.sql" `
    -c "SELECT pg_temp.qa_assert_state('Scheduled', false, false);"
if ($LASTEXITCODE -ne 0) { throw 'P1 baseline mismatch; stop' }
```

The helper includes temporary expected rows and a temporary assertion function;
it does not seed persistent tables. It compares every value and row count in all
nine Trip tables, including timestamps, status, fares, seats/coordinates and all
other original configuration. Extra Trip data is rejected, so it cannot contaminate
search counts. Short table locks prevent concurrent writes during each operation;
the database must still remain free of writers between operations. These locks
are another reason never to use the scripts on a shared database.

Each pair accepts exactly one transition: baseline to scenario, then that exact
scenario to baseline. Repeating apply or restore fails loudly, including an
already-restored row, wrong status, wrong date, changed seat coordinates or fare.
Every persistent DML statement must affect exactly one row. No upsert or silent
zero-row success is used. The single transaction rolls back on error; success
requires process exit code 0, including commit. A printed message alone is not
proof of commit. Missing variables and invalid status/date/target also fail.

Run one scenario at a time. Successful apply commits before HTTP requests on
separate app connections; an uncommitted change in a SQL client is not visible
to that app. Restore with the paired script immediately after evidence capture,
then run the independent baseline check above. If apply fails, stop: do not
blindly run restore. If restore fails, do not force values, disable constraints or
delete dependent records. Inspect the mismatch; if necessary, retire and rebuild
only the disposable environment through the existing seed/reset guide.

## HTTP templates and common assertions

Use a running backend with the recorded datasource; UI checks also require the
frontend. In these URL templates replace `<BASE>` with the app origin, `<D>` with
the stored date, and `<FROM>`/`<TO>` with exact A/B/C UUIDs from the table.
No session, CSRF or client-supplied clock is required for these public GETs.

```text
P2: GET <BASE>/api/v1/trips?fromStopId=<FROM>&toStopId=<TO>&boardingDate=<D>&page=0&size=20
P4: GET <BASE>/api/v1/trips/11111111-1111-4111-8111-111111111111?fromStopId=<FROM>&toStopId=<TO>
```

P2 cannot filter by Trip ID. Before day cutoff, baseline A-C on D returns exactly
day then overnight, both 250000; A-B returns both at 100000. P4 baseline A-C has
`status=Scheduled`, three ordered stops, two legFares, fare 250000,
`bookingEligibility={allowed:true,reasons:[]}`, explicit `description:null`.
Expect `Cache-Control: no-store`, UTC timestamps ending in Z and authoritative
`serverTime` on responses. Validate schema keys/nullability, not exact display messages.
Missing data is not a fabricated zero fare or a null-timestamp P2 result item.

P2 rejects a past boarding date with 422 `PAST_BOARDING_DATE` before these result
assertions. A potential matching open candidate with incomplete essential facts
fails the whole search with 503 `TRIP_DATA_UNAVAILABLE`, even outside the requested
page. Provably nonscheduled, closed or wrong-date Trips are excluded instead.
An actual retrieval failure is 503 for both APIs, not P4 partial success.

P4 preserves verified facts on an existing Trip and accumulates applicable reasons
in order: `TRIP_NOT_SCHEDULED`, `BOOKING_CLOSED`, `ESSENTIAL_DATA_INCOMPLETE`.
The single-reason expectations below require all other baseline conditions to hold.
P4 eligibility does not promise a free seat or a successful future reservation.

## Scenario 1: each closed status separately

Prerequisites: full clean baseline, recorded D, day Trip still before D 08:30 VN;
use the P2/P4 templates with A-C on D. The same exact day UUID is changed in each
run. Restore and verify baseline before moving to the next status:

```powershell
Invoke-QaScenarioSql 'status-apply.sql' 'Cancelled'
# Capture P2/P4/UI evidence, then:
Invoke-QaScenarioSql 'status-restore.sql' 'Cancelled'
# Run the independent baseline check before continuing.
Invoke-QaScenarioSql 'status-apply.sql' 'Departed'
# Capture evidence, then:
Invoke-QaScenarioSql 'status-restore.sql' 'Departed'
# Run the independent baseline check before continuing.
Invoke-QaScenarioSql 'status-apply.sql' 'Completed'
# Capture evidence, then:
Invoke-QaScenarioSql 'status-restore.sql' 'Completed'
# Run the independent baseline check.
```

Predicted P2: 200, day UUID absent; exactly overnight remains at 250000, totals
1/1. Predicted P4: 200, requested status retained, verified A-C Segment/price/times
and other facts retained; allowed false, reasons exactly `[TRIP_NOT_SCHEDULED]`.
After restore, both Scheduled Trips return again if still open. No timestamp is
changed: Departed/Completed before cutoff are synthetic schema-valid status tests.

## Scenario 2: Scheduled at/after first-stop cutoff

Prerequisites: full clean baseline, no SQL mutation, recorded stored cutoff and
application time. Use A-C on D in both templates. Compare asOf at cutoff minus
1 ms, exactly cutoff, and plus 1 ms: predicted P2 returns both Trips before day
cutoff, then only overnight (totals 1/1); P4 day stays 200 with Scheduled and fare
250000, but allowed false and reasons `[BOOKING_CLOSED]` at equality and afterward.

For P2 `boardingDate=D`, these search assertions require the server-local date
in `Asia/Ho_Chi_Minh` to be on or before D. The day-closed/overnight-present
result also requires serverTime to remain before the overnight D 22:00 VN cutoff.
From that cutoff through the end of D, the complete baseline yields empty results.
Once the server-local date is later than D, P2 may instead return 422
`PAST_BOARDING_DATE`; do not assert these search results outside their prerequisites.

For an isolated empty-result check, P2 B-C on **D+1** returns only overnight before
its D 22:00 VN cutoff, then 200 with `items:[]`, `totalElements:0`, `totalPages:0`.
For P4 use the same B-C query pair but replace the path Trip ID with overnight
`11111111-1111-4111-8111-111111111112`; at/after cutoff it remains 200, false,
`[BOOKING_CLOSED]`, fare 150000. B's later departure does not extend the cutoff.

For P2 `boardingDate=D+1`, the search date remains valid while the server-local
Vietnam date is on or before D+1. Thus the post-cutoff empty-result prediction
applies from D 22:00 VN through the end of D+1, with the clean baseline unchanged.
Once the server-local date is later than D+1, P2 may instead return 422
`PAST_BOARDING_DATE`; do not assert an empty 200 then. P4 has no boardingDate
parameter, so this P2 past-date validation does not apply to its details request.

Production P2/P4 use `Clock.systemUTC()`. Existing tests replace Clock beans, but
there is **no documented runtime clock injection switch or client parameter**.
Existing automated controlled-clock tests cover cutoff minus 1 ms, equality and
plus 1 ms; they were inspected and have **not been rerun for this task**. Those
automated boundary assertions are distinct from manual runtime HTTP observations
using the actual server clock. A manual HTTP run does not verify millisecond
boundaries; record only the observed serverTime and its relation to cutoff.
Exact equality/1 ms runtime observations require an explicitly arranged test-only
server clock harness, outside this pack's implementation scope. Without that
harness, wait for the real cutoff and classify observations using returned
`serverTime`; record equality as not executed rather than claiming precision.
Never change the host clock or rewrite TripStop/Schedule times to fake this case.
No SQL restore applies. If a harness was used, reset it and verify baseline/time;
once real time passes cutoff, SQL restoration cannot reopen the Trip.

## Scenario 3: missing fare inside selected Segment

Prerequisites: full clean baseline, current date not after D and server before day
cutoff. Delete only day B-C with the pair below. Both A-C and B-C exercise the
missing leg; use those pairs in each HTTP template with boardingDate D.

```powershell
Invoke-QaScenarioSql 'fare-bc-apply.sql'
# Capture A-C and B-C P2/P4/UI evidence.
Invoke-QaScenarioSql 'fare-bc-restore.sql'
# Run the independent baseline check.
```

Predicted P2: 503 with `code=TRIP_DATA_UNAVAILABLE`, not 200/empty or just overnight.
Predicted P4: 200, Scheduled, allowed false, `[ESSENTIAL_DATA_INCOMPLETE]`;
selectedSegment retains correct UUIDs/times but `fareAmountVnd:null`.
`legFares` contains only A-B at 100000. Restore inserts the exact day B-C key at
150000; verify two fares/day, baseline responses and restored selected prices
A-C=250000, B-C=150000 while open.

## Scenario 4: missing B-C while selecting A-B

Prerequisites and commands are the same pair as scenario 3, run separately from
a verified baseline. Use A-B on D in both HTTP templates.

Predicted P2: 503 `TRIP_DATA_UNAVAILABLE`, because completeness is checked across
the entire Trip, not just the selected Segment. Predicted P4: 200, verified A-B
fare **100000** and its original times retained, one A-B legFare, allowed false,
reasons `[ESSENTIAL_DATA_INCOMPLETE]`. Do not assert selectedSegment or its fare
is null here. Restore via `fare-bc-restore.sql`, run baseline check and verify
A-B still 100000 with eligibility true before cutoff.

## Scenario 5: physical seat C2 missing

Prerequisites: full clean baseline, no Booking data, server before day cutoff.
Use A-C on D in both templates. This mutation affects **both Trips**, since they
share the Vehicle; it does not create or change a layout UUID.

```powershell
Invoke-QaScenarioSql 'seat-c2-apply.sql'
# Capture P2/P4/UI evidence for day; also request overnight P4 using its path ID.
Invoke-QaScenarioSql 'seat-c2-restore.sql'
# Run the independent baseline check.
```

Predicted P2: 503 `TRIP_DATA_UNAVAILABLE`. Predicted P4 for each open Trip: 200,
Vehicle/layout identity retained, selected A-C fare 250000/times retained,
allowed false and `[ESSENTIAL_DATA_INCOMPLETE]`. The P4 response does not contain
a seat list; verify five physical rows against unchanged capacity six in SQL,
not through an invented response field. Restore must recreate C2's exact UUID,
Vehicle, code and `(0,2,2)` coordinates; the guard checks all six seat rows and
unchanged configuration afterward. Verify both Trips return in P2 again and P4
eligibility true while open.

## Scenario 6: description null

Prerequisites: full clean baseline, server before day cutoff. No SQL apply or
restore exists or is needed. Use A-C on D in both templates.

Predicted P2: unchanged baseline 200, both Trips at 250000. Predicted P4: 200,
explicit `description:null` (key present), allowed true with empty reasons;
Segment fare 250000 remains verified. Omitting the optional Stop pair instead
must yield `selectedSegment:null` without itself disabling eligibility. Hoang
should check the UI's unavailable-description display and that this null alone
does not disable continuing. Finish with the baseline check; no data was changed.

## Constraint boundaries and unsafe alternatives

- Status is NOT NULL and checked by `trip_status_valid`; only the three validated
  case-sensitive status parameters are accepted. Restore requires that same status.
- Cutoff is calculated from `trip_stop.planned_departure_time` at
  `route_stop.stop_order=0`, never Schedule or `trip.departure_date`. There is no
  actual-departure/cutoff column to update. `trip_stop_time_required` requires at
  least one time, and `trip_stop_time_valid` checks arrival <= departure.
- Fare has a composite PK, NOT NULL BIGINT amount, `leg_fare_amount_nonnegative`
  and `leg_fare_distinct_stops` checks. The schema permits missing consecutive
  rows; it does not enforce full coverage/adjacency. Zero fare is valid, not missing.
  The endpoint FKs `leg_fare_from_trip_stop_fk` and `leg_fare_to_trip_stop_fk`
  prohibit deleting TripStop B while keeping its referenced fares. Do not attempt it.
- `trip_vehicle_fk` requires an assigned existing Vehicle. `seat_layout_id` is
  NOT NULL/unique, `layout_template` is NOT NULL/nonblank, and capacity is positive.
  There is no separate seat_layout table. Null metadata or deleting the assigned
  Vehicle is not a V3-valid missing-layout fixture. Physical seats have stable PKs,
  `physical_seat_vehicle_fk`, unique labels/positions and nonnegative coordinates;
  no check enforces seat count = capacity, so the C2 scenario is valid under V3.
  V4's `booking_seat_physical_seat_fk` may block seat deletion if Booking data
  exists. Abort; never delete Booking/SeatAllocation or weaken constraints.
- V3 has no description source; P4 deliberately emits null. Do not invent a field.
  These scripts never disable/defer constraints, cascade deletion or touch Booking
  tables. Do not combine scenarios: the strict guard rejects overlapping damage.

## Evidence and completion record

Duc Hieu should keep one record per status/subcase. Leave actual results blank
until execution; do not copy predictions or historical test reports as PASS.

| Evidence field | Value to record |
|---|---|
| Scenario/subcase, operator, execution time | VN time and UTC; no invented Q1 Case IDs |
| Environment | Confirmed isolated host/port/database (redact credentials), app commit, P4 PR dependency |
| Baseline | Recorded D, original six TripStop times, cutoff, baseline-check exit code |
| Preparation | Script/validated status, psql exit code, exact affected row count 1 |
| Clock | Real clock or named harness, actual serverTime, relation to stored cutoff |
| Request | Exact URL/pair/boardingDate/page; no auth/secret data |
| Expected versus actual | HTTP status, no-store, body/assertions; PASS/FAIL/NOT RUN with evidence reference |
| UI | Hoang's observed status, verified fare/null description display, booking controls; separate from API evidence |
| Restoration | Paired command/exit code, independent baseline-check exit code, exact restored status/fare/C2 values |
| Post-restore API | Result and serverTime; distinguish time-caused closure from failed data restoration |

Static source coverage exists in P2 `PostgresTripSearchRepositoryIT` and
`TripSearchPolicyTest`, and on the P4 branch in `PostgresTripDetailsRepositoryIT`
and `TripDetailsPolicyTest`. Those tests were inspected, not executed for this
pack. The PostgreSQL execution evidence below verifies the recorded SQL scenarios;
live HTTP responses, exact clock boundaries and frontend behavior remain unverified.

## PostgreSQL execution evidence — user's terminal

Environment: PostgreSQL **16.16** in isolated Docker container
`team11-trip-qa-db`, database `team11_trip_qa`. Flyway **11.20.3** successfully
applied V1–V4. The original P1 runtime seed executed successfully and was verified;
recorded seed date **D = 2026-11-19**. Results below are transcribed from the
user's supplied terminal evidence, not an agent rerun.

| Executed operation | Result | Evidence type |
|---|---|---|
| Initial baseline: `_guard.sql` and `qa_assert_state('Scheduled', false, false)` | PASS | Standalone baseline assertion, independent of apply/restore scripts |
| Cancelled apply | PASS | Script assertions; additionally, a direct independent query showed Day Trip Cancelled and Overnight Scheduled |
| Cancelled restore | PASS | Script assertions; subsequent standalone independent baseline verification PASS |
| Departed apply / restore | PASS / PASS | Assertions inside the paired scripts |
| Completed apply / restore | PASS / PASS | Assertions inside the paired scripts |
| Missing Day Trip B–C fare apply / restore | PASS / PASS | Assertions inside the paired scripts |
| Missing shared Vehicle C2 seat apply / restore | PASS / PASS | Assertions inside the paired scripts |
| Final independent baseline verification across all nine Trip tables | PASS | Standalone baseline assertion, independent of apply/restore scripts |

The direct status query is separate evidence from the scripts' pre/post assertions.
The standalone baseline checks also use the guard/assertion mechanism; they are
not nine separate direct table-query checks. Independent baseline verification
was executed only initially, after Cancelled restore, and finally. No separate
independent baseline check is claimed for every other transition.

**NOT RUN:** HTTP P2/P4 requests, exact cutoff-boundary runtime checks and UI
acceptance. Existing automated tests, including controlled-clock boundary tests,
were not rerun as part of this QA pack. Database PASS results do not verify HTTP
contracts, millisecond clock boundaries or frontend behavior. P4 HTTP acceptance
still requires an app containing PR #13; all existing HTTP expectations remain
predictions.
