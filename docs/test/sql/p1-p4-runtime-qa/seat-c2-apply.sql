-- Isolated disposable P1-only database ONLY. See ../../p1-p4-runtime-qa-scenarios.md.
-- Requires psql -X -v ON_ERROR_STOP=1 -1 and the documented qa_* variables.
-- Strict one-shot transition: repeat execution fails; no ON CONFLICT/no-op success.
\ir _guard.sql
SELECT pg_temp.qa_assert_state('Scheduled', false, false);
DO $mutation$
DECLARE affected bigint;
BEGIN
    DELETE FROM public.physical_seat
    WHERE physical_seat_id = '77777777-7777-4777-8777-777777777781' AND vehicle_id = '66666666-6666-4666-8666-666666666666'
      AND seat_code = 'C2' AND deck = 0 AND seat_row = 2 AND seat_column = 2;
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Expected exactly 1 seat delete, got %', affected; END IF;
END;
$mutation$;
SELECT pg_temp.qa_assert_state('Scheduled', false, true);
\echo Exact expected seat state verified; success requires psql exit code 0.
