-- Isolated disposable P1-only database ONLY. See ../../p1-p4-runtime-qa-scenarios.md.
-- Requires psql -X -v ON_ERROR_STOP=1 -1 and the documented qa_* variables.
-- Strict one-shot transition: repeat execution fails; no ON CONFLICT/no-op success.
\ir _guard.sql
SELECT pg_temp.qa_assert_state('Scheduled', false, false);
DO $mutation$
DECLARE affected bigint;
BEGIN
    DELETE FROM public.leg_fare
    WHERE trip_id = '11111111-1111-4111-8111-111111111111' AND from_stop_id = '33333333-3333-4333-8333-333333333333'
      AND to_stop_id = '99999999-9999-4999-8999-999999999999' AND fare_amount_vnd = 150000;
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Expected exactly 1 fare delete, got %', affected; END IF;
END;
$mutation$;
SELECT pg_temp.qa_assert_state('Scheduled', true, false);
\echo Exact expected fare state verified; success requires psql exit code 0.
