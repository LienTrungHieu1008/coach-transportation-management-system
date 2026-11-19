-- Internal include; run a paired script with psql -X -v ON_ERROR_STOP=1 -1.
-- CONFIRMED ISOLATED DISPOSABLE P1-ONLY DATABASE, no Booking data or writers.
-- No permanent helper objects; expected rows and function live in pg_temp.
\set ON_ERROR_STOP on
SET LOCAL search_path TO public, pg_temp;
SET LOCAL lock_timeout = '5s';
SELECT set_config('qa.database', :'qa_database', true),
       set_config('qa.seed_date', :'qa_seed_date', true),
       set_config('qa.isolated', :'qa_isolated', true);
DO $guard$
BEGIN
    IF current_setting('qa.isolated') <> 'CONFIRMED_ISOLATED_DISPOSABLE'
       OR current_setting('qa.database') = ''
       OR current_database() <> current_setting('qa.database') THEN
        RAISE EXCEPTION 'Confirm isolated disposable target and exact database name';
    END IF;
    IF current_setting('qa.seed_date') !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
       OR NOT isfinite(current_setting('qa.seed_date')::date) THEN
        RAISE EXCEPTION 'qa_seed_date must be the actual seeded departure date YYYY-MM-DD';
    END IF;
END;
$guard$;

-- A single transaction is mandatory: ON COMMIT DROP also prevents accidental
-- autocommit use from reaching persistent mutations. Reads by the app remain possible.
LOCK TABLE public.route, public.stop, public.route_stop, public.vehicle,
    public.physical_seat, public.schedule, public.trip, public.trip_stop,
    public.leg_fare IN SHARE ROW EXCLUSIVE MODE;
CREATE TEMP TABLE qa_expected_route (LIKE public.route) ON COMMIT DROP;
CREATE TEMP TABLE qa_expected_stop (LIKE public.stop) ON COMMIT DROP;
CREATE TEMP TABLE qa_expected_route_stop (LIKE public.route_stop) ON COMMIT DROP;
CREATE TEMP TABLE qa_expected_vehicle (LIKE public.vehicle) ON COMMIT DROP;
CREATE TEMP TABLE qa_expected_physical_seat (LIKE public.physical_seat) ON COMMIT DROP;
CREATE TEMP TABLE qa_expected_schedule (LIKE public.schedule) ON COMMIT DROP;
CREATE TEMP TABLE qa_expected_trip (LIKE public.trip) ON COMMIT DROP;
CREATE TEMP TABLE qa_expected_trip_stop (LIKE public.trip_stop) ON COMMIT DROP;
CREATE TEMP TABLE qa_expected_leg_fare (LIKE public.leg_fare) ON COMMIT DROP;

-- Exact P1 runtime values. D is supplied from the recorded seed, never today's date.
INSERT INTO qa_expected_route VALUES ('44444444-4444-4444-8444-444444444444', 'DEMO-ABC', 'Demo Route A-B-C', TRUE);
INSERT INTO qa_expected_stop VALUES
    ('22222222-2222-4222-8222-222222222222', 'Demo Stop A', 'Demo Address A', TRUE),
    ('33333333-3333-4333-8333-333333333333', 'Demo Stop B', 'Demo Address B', TRUE),
    ('99999999-9999-4999-8999-999999999999', 'Demo Stop C', 'Demo Address C', TRUE);
INSERT INTO qa_expected_route_stop VALUES
    ('44444444-4444-4444-8444-444444444444', '22222222-2222-4222-8222-222222222222', 0),
    ('44444444-4444-4444-8444-444444444444', '33333333-3333-4333-8333-333333333333', 1),
    ('44444444-4444-4444-8444-444444444444', '99999999-9999-4999-8999-999999999999', 2);
INSERT INTO qa_expected_vehicle VALUES
    ('66666666-6666-4666-8666-666666666666', 'DEMO-VEHICLE-01', '47B-123.45', 'Demo coach', 6,
     'SEATED_6_1_DECK', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', TRUE);
INSERT INTO qa_expected_physical_seat VALUES
    ('77777777-7777-4777-8777-777777777777', '66666666-6666-4666-8666-666666666666', 'A1', 0, 0, 0),
    ('88888888-8888-4888-8888-888888888888', '66666666-6666-4666-8666-666666666666', 'A2', 0, 0, 2),
    ('77777777-7777-4777-8777-777777777778', '66666666-6666-4666-8666-666666666666', 'B1', 0, 1, 0),
    ('77777777-7777-4777-8777-777777777779', '66666666-6666-4666-8666-666666666666', 'B2', 0, 1, 2),
    ('77777777-7777-4777-8777-777777777780', '66666666-6666-4666-8666-666666666666', 'C1', 0, 2, 0),
    ('77777777-7777-4777-8777-777777777781', '66666666-6666-4666-8666-666666666666', 'C2', 0, 2, 2);
INSERT INTO qa_expected_schedule VALUES
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa10', '44444444-4444-4444-8444-444444444444', 'DAILY', TIME '09:00:00'),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa11', '44444444-4444-4444-8444-444444444444', 'DAILY', TIME '22:30:00');
INSERT INTO qa_expected_trip VALUES
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444444', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa10',
     '66666666-6666-4666-8666-666666666666', current_setting('qa.seed_date')::date, 'Scheduled'),
    ('11111111-1111-4111-8111-111111111112', '44444444-4444-4444-8444-444444444444', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa11',
     '66666666-6666-4666-8666-666666666666', current_setting('qa.seed_date')::date, 'Scheduled');
INSERT INTO qa_expected_trip_stop
    (trip_id, route_id, stop_id, planned_arrival_time, planned_departure_time)
SELECT t.trip_id, t.route_id, s.stop_id,
       CASE WHEN s.stop_order = 0 THEN NULL
            ELSE (t.departure_date + x.day_offset + x.local_time) AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
       CASE WHEN s.stop_order = 2 THEN NULL
            ELSE (t.departure_date + x.day_offset + x.local_time) AT TIME ZONE 'Asia/Ho_Chi_Minh' END
FROM qa_expected_trip t
JOIN qa_expected_route_stop s ON s.route_id = t.route_id
JOIN (VALUES
    ('11111111-1111-4111-8111-111111111111'::uuid, 0, 0, TIME '09:00:00'),
    ('11111111-1111-4111-8111-111111111111'::uuid, 1, 0, TIME '11:00:00'),
    ('11111111-1111-4111-8111-111111111111'::uuid, 2, 0, TIME '13:00:00'),
    ('11111111-1111-4111-8111-111111111112'::uuid, 0, 0, TIME '22:30:00'),
    ('11111111-1111-4111-8111-111111111112'::uuid, 1, 1, TIME '00:30:00'),
    ('11111111-1111-4111-8111-111111111112'::uuid, 2, 1, TIME '02:30:00')
) x(trip_id, stop_order, day_offset, local_time)
ON x.trip_id = t.trip_id AND x.stop_order = s.stop_order;
INSERT INTO qa_expected_leg_fare
SELECT t.trip_id, x.from_id, x.to_id, x.amount
FROM qa_expected_trip t
CROSS JOIN (VALUES
    ('22222222-2222-4222-8222-222222222222'::uuid, '33333333-3333-4333-8333-333333333333'::uuid, 100000::bigint),
    ('33333333-3333-4333-8333-333333333333'::uuid, '99999999-9999-4999-8999-999999999999'::uuid, 150000::bigint)
) x(from_id, to_id, amount);

-- Compare ALL rows of the nine Trip tables; reject extra data as well as
-- missing/changed values. Only the explicitly requested scenario delta is allowed.
CREATE FUNCTION pg_temp.qa_assert_state(
    day_status text, missing_bc boolean, missing_c2 boolean
) RETURNS void LANGUAGE plpgsql AS $check$
DECLARE
    table_name text;
    actual_rows jsonb;
    expected_rows jsonb;
BEGIN
    IF day_status IS NULL OR day_status NOT IN ('Scheduled', 'Cancelled', 'Departed', 'Completed')
       OR missing_bc IS NULL OR missing_c2 IS NULL THEN
        RAISE EXCEPTION 'Invalid QA expected state';
    END IF;
    FOREACH table_name IN ARRAY ARRAY[
        'route', 'stop', 'route_stop', 'vehicle', 'physical_seat',
        'schedule', 'trip', 'trip_stop', 'leg_fare'
    ] LOOP
        EXECUTE format(
            'SELECT coalesce(jsonb_agg(to_jsonb(x) ORDER BY to_jsonb(x)), ''[]''::jsonb) FROM public.%I x',
            table_name) INTO actual_rows;
        EXECUTE format($query$
            SELECT coalesce(jsonb_agg(row_value ORDER BY row_value), '[]'::jsonb)
            FROM (
                SELECT CASE WHEN $1 = 'trip' AND x->>'trip_id' = '11111111-1111-4111-8111-111111111111'
                       THEN jsonb_set(x, '{status}', to_jsonb($2::text)) ELSE x END AS row_value
                FROM (SELECT to_jsonb(e) AS x FROM pg_temp.%I e) raw
                WHERE NOT ($3 AND $1 = 'leg_fare'
                    AND x->>'trip_id' = '11111111-1111-4111-8111-111111111111'
                    AND x->>'from_stop_id' = '33333333-3333-4333-8333-333333333333'
                    AND x->>'to_stop_id' = '99999999-9999-4999-8999-999999999999')
                  AND NOT ($4 AND $1 = 'physical_seat'
                    AND x->>'physical_seat_id' = '77777777-7777-4777-8777-777777777781')
            ) expected
            $query$, 'qa_expected_' || table_name)
            INTO expected_rows USING table_name, day_status, missing_bc, missing_c2;
        IF actual_rows IS DISTINCT FROM expected_rows THEN
            RAISE EXCEPTION 'Unexpected state in %; abort, do not overwrite or force restore', table_name;
        END IF;
    END LOOP;
END;
$check$;
