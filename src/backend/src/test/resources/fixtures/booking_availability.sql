-- Synthetic Booking fixtures only; compose after the owners' Account and Trip fixtures.
-- Seed once in a fresh isolated local/test transaction; duplicate IDs deliberately fail.
WITH seed AS (SELECT TIMESTAMPTZ '2026-11-19T00:08:00Z' AS seed_time),
rows (booking_id, from_stop_id, to_stop_id, from_index, to_index, status, fare, age_minutes) AS (
    VALUES
    ('dddddddd-dddd-4ddd-8ddd-dddddddddd01'::uuid, '22222222-2222-4222-8222-222222222222'::uuid,
     '33333333-3333-4333-8333-333333333333'::uuid, 0, 1, 'Confirmed', 100000, 28),
    ('dddddddd-dddd-4ddd-8ddd-dddddddddd02'::uuid, '22222222-2222-4222-8222-222222222222'::uuid,
     '99999999-9999-4999-8999-999999999999'::uuid, 0, 2, 'Confirmed', 250000, 28),
    ('dddddddd-dddd-4ddd-8ddd-dddddddddd03'::uuid, '33333333-3333-4333-8333-333333333333'::uuid,
     '99999999-9999-4999-8999-999999999999'::uuid, 1, 2, 'Pending', 150000, 8),
    ('dddddddd-dddd-4ddd-8ddd-dddddddddd04'::uuid, '22222222-2222-4222-8222-222222222222'::uuid,
     '99999999-9999-4999-8999-999999999999'::uuid, 0, 2, 'Pending', 250000, 28),
    ('dddddddd-dddd-4ddd-8ddd-dddddddddd05'::uuid, '22222222-2222-4222-8222-222222222222'::uuid,
     '99999999-9999-4999-8999-999999999999'::uuid, 0, 2, 'Cancelled', 250000, 8)
)
INSERT INTO booking (booking_id, passenger_account_id, trip_id, from_stop_id, to_stop_id,
    from_stop_index, to_stop_index, status, total_amount_vnd, hold_created_at, expires_at)
SELECT booking_id, 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1', '11111111-1111-4111-8111-111111111111',
    from_stop_id, to_stop_id, from_index, to_index, status, fare,
    seed_time - age_minutes * INTERVAL '1 minute',
    seed_time - age_minutes * INTERVAL '1 minute' + INTERVAL '10 minutes'
FROM rows CROSS JOIN seed;

INSERT INTO booking_seat (booking_seat_id, booking_id, physical_seat_id, traveller_name, fare_amount_vnd)
VALUES
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01', 'dddddddd-dddd-4ddd-8ddd-dddddddddd01',
     '88888888-8888-4888-8888-888888888888', 'Synthetic Traveller Two', 100000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02', 'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
     '77777777-7777-4777-8777-777777777778', 'Synthetic Traveller Three', 250000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03', 'dddddddd-dddd-4ddd-8ddd-dddddddddd03',
     '77777777-7777-4777-8777-777777777779', 'Synthetic Traveller Four', 150000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee04', 'dddddddd-dddd-4ddd-8ddd-dddddddddd04',
     '77777777-7777-4777-8777-777777777780', 'Synthetic Traveller Five', 250000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee05', 'dddddddd-dddd-4ddd-8ddd-dddddddddd05',
     '77777777-7777-4777-8777-777777777781', 'Synthetic Traveller Six', 250000);

INSERT INTO seat_allocation (seat_allocation_id, booking_seat_id, booking_id, trip_id, physical_seat_id,
    from_stop_index, to_stop_index, kind, expires_at, released_at)
SELECT s.booking_seat_id, s.booking_seat_id, b.booking_id, b.trip_id, s.physical_seat_id,
    b.from_stop_index, b.to_stop_index,
    CASE WHEN b.status = 'Confirmed' THEN 'Confirmed' ELSE 'Hold' END,
    CASE WHEN b.booking_id = 'dddddddd-dddd-4ddd-8ddd-dddddddddd01' THEN NULL ELSE b.expires_at END,
    CASE WHEN b.status = 'Cancelled' THEN b.hold_created_at + INTERVAL '7 minutes' ELSE NULL END
FROM booking b JOIN booking_seat s USING (booking_id)
WHERE b.booking_id IN ('dddddddd-dddd-4ddd-8ddd-dddddddddd01', 'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
    'dddddddd-dddd-4ddd-8ddd-dddddddddd03', 'dddddddd-dddd-4ddd-8ddd-dddddddddd04',
    'dddddddd-dddd-4ddd-8ddd-dddddddddd05');
