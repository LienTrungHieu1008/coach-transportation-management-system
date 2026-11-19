-- Isolated disposable P1-only database ONLY. See ../../p1-p4-runtime-qa-scenarios.md.
-- Requires psql -X -v ON_ERROR_STOP=1 -1 and the documented qa_* variables.
-- Strict one-shot transition: repeat execution fails; no ON CONFLICT/no-op success.
\ir _guard.sql
SELECT pg_temp.qa_assert_state('Scheduled', true, false);
DO $mutation$
DECLARE affected bigint;
BEGIN
    INSERT INTO public.leg_fare (trip_id, from_stop_id, to_stop_id, fare_amount_vnd)
    VALUES ('11111111-1111-4111-8111-111111111111', '33333333-3333-4333-8333-333333333333', '99999999-9999-4999-8999-999999999999', 150000);
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Expected exactly 1 fare insert, got %', affected; END IF;
END;
$mutation$;
SELECT pg_temp.qa_assert_state('Scheduled', false, false);
\echo Exact expected fare state verified; success requires psql exit code 0.
