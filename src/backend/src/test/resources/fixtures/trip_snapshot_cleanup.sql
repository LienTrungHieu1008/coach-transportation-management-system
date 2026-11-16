-- Only for the isolated PostgreSQL integration-test database.
-- The snapshot test commits trip_demo.sql so another connection can see it.
-- Remove precisely that fixture in FK order, including after a failed test.
DELETE FROM leg_fare WHERE trip_id IN (
    '11111111-1111-4111-8111-111111111111', '11111111-1111-4111-8111-111111111112');
DELETE FROM trip_stop WHERE trip_id IN (
    '11111111-1111-4111-8111-111111111111', '11111111-1111-4111-8111-111111111112');
DELETE FROM trip WHERE trip_id IN (
    '11111111-1111-4111-8111-111111111111', '11111111-1111-4111-8111-111111111112');
DELETE FROM schedule WHERE schedule_id IN (
    'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa10', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa11');
DELETE FROM physical_seat WHERE vehicle_id = '66666666-6666-4666-8666-666666666666';
DELETE FROM vehicle WHERE vehicle_id = '66666666-6666-4666-8666-666666666666';
DELETE FROM route_stop WHERE route_id = '44444444-4444-4444-8444-444444444444';
DELETE FROM route WHERE route_id = '44444444-4444-4444-8444-444444444444';
DELETE FROM stop WHERE stop_id IN (
    '22222222-2222-4222-8222-222222222222',
    '33333333-3333-4333-8333-333333333333',
    '99999999-9999-4999-8999-999999999999');
