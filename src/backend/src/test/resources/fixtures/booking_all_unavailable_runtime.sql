-- Synthetic sold-out scenario; use instead of booking_availability_runtime.sql on a fresh demo DB.
-- Compose Account, Trip and this fixture in one psql transaction.
INSERT INTO booking (booking_id, passenger_account_id, trip_id, from_stop_id, to_stop_id,
    from_stop_index, to_stop_index, status, total_amount_vnd, hold_created_at, expires_at)
VALUES ('dddddddd-dddd-4ddd-8ddd-dddddddddd02', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
    '11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222',
    '99999999-9999-4999-8999-999999999999', 0, 2, 'Confirmed', 1500000,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 minutes');

INSERT INTO booking_seat (booking_seat_id, booking_id, physical_seat_id, traveller_name, fare_amount_vnd)
VALUES
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01', 'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
     '77777777-7777-4777-8777-777777777777', 'Synthetic Traveller One', 250000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02', 'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
     '88888888-8888-4888-8888-888888888888', 'Synthetic Traveller Two', 250000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03', 'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
     '77777777-7777-4777-8777-777777777778', 'Synthetic Traveller Three', 250000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee04', 'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
     '77777777-7777-4777-8777-777777777779', 'Synthetic Traveller Four', 250000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee05', 'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
     '77777777-7777-4777-8777-777777777780', 'Synthetic Traveller Five', 250000),
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeee06', 'dddddddd-dddd-4ddd-8ddd-dddddddddd02',
     '77777777-7777-4777-8777-777777777781', 'Synthetic Traveller Six', 250000);

INSERT INTO seat_allocation (seat_allocation_id, booking_seat_id, booking_id, trip_id, physical_seat_id,
    from_stop_index, to_stop_index, kind, expires_at, released_at)
SELECT s.booking_seat_id, s.booking_seat_id, b.booking_id, b.trip_id, s.physical_seat_id,
    b.from_stop_index, b.to_stop_index, 'Confirmed', NULL, NULL
FROM booking b JOIN booking_seat s USING (booking_id)
WHERE b.booking_id = 'dddddddd-dddd-4ddd-8ddd-dddddddddd02';
