-- Isolated disposable P1-only database ONLY. See ../../p1-p4-runtime-qa-scenarios.md.
-- Requires psql -X -v ON_ERROR_STOP=1 -1 and the documented qa_* variables.
-- Strict one-shot transition: repeat execution fails; no ON CONFLICT/no-op success.
\ir _guard.sql
SELECT pg_temp.qa_assert_state('Scheduled', false, true);
DO $mutation$
DECLARE affected bigint;
BEGIN
    INSERT INTO public.physical_seat
        (physical_seat_id, vehicle_id, seat_code, deck, seat_row, seat_column)
    VALUES ('77777777-7777-4777-8777-777777777781', '66666666-6666-4666-8666-666666666666', 'C2', 0, 2, 2);
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Expected exactly 1 seat insert, got %', affected; END IF;
END;
$mutation$;
SELECT pg_temp.qa_assert_state('Scheduled', false, false);
\echo Exact expected seat state verified; success requires psql exit code 0.
