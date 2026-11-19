-- Isolated disposable P1-only database ONLY. See ../../p1-p4-runtime-qa-scenarios.md.
-- Requires psql -X -v ON_ERROR_STOP=1 -1 and the documented qa_* variables.
-- Strict one-shot transition: repeat execution fails; no ON CONFLICT/no-op success.
\ir _guard.sql
SELECT set_config('qa.status', :'qa_status', true);
DO $validate$
BEGIN
    IF current_setting('qa.status') NOT IN ('Cancelled', 'Departed', 'Completed') THEN
        RAISE EXCEPTION 'qa_status must be exactly Cancelled, Departed, or Completed';
    END IF;
END;
$validate$;
SELECT pg_temp.qa_assert_state('Scheduled', false, false);
DO $mutation$
DECLARE affected bigint;
BEGIN
    UPDATE public.trip SET status = current_setting('qa.status')
    WHERE trip_id = '11111111-1111-4111-8111-111111111111' AND status = 'Scheduled';
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Expected exactly 1 Trip update, got %', affected; END IF;
END;
$mutation$;
SELECT pg_temp.qa_assert_state(current_setting('qa.status'), false, false);
\echo Exact expected status state verified; success requires psql exit code 0.
