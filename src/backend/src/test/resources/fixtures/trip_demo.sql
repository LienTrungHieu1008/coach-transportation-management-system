
-- =========================================================
-- P1 TRIP DEMO FIXTURE - TEAM 11
-- Owner: Do Minh Phuong
-- Local/test database only.
-- Requires Flyway V3.
-- Stable UUIDs shared with Huy's V4.
-- =========================================================

-- 1. ROUTE A -> B -> C

INSERT INTO route (
    route_id, route_code, route_name, is_active
) VALUES (
    '44444444-4444-4444-8444-444444444444',
    'DEMO-ABC',
    'Demo Route A-B-C',
    TRUE
);

-- 2. THREE STOPS

INSERT INTO stop (
    stop_id, stop_name, address, is_active
) VALUES
(
    '22222222-2222-4222-8222-222222222222',
    'Demo Stop A', 'Demo Address A', TRUE
),
(
    '33333333-3333-4333-8333-333333333333',
    'Demo Stop B', 'Demo Address B', TRUE
),
(
    '99999999-9999-4999-8999-999999999999',
    'Demo Stop C', 'Demo Address C', TRUE
);

-- 3. ROUTE STOP ORDER - ZERO BASED

INSERT INTO route_stop (
    route_id, stop_id, stop_order
) VALUES
(
    '44444444-4444-4444-8444-444444444444',
    '22222222-2222-4222-8222-222222222222',
    0
),
(
    '44444444-4444-4444-8444-444444444444',
    '33333333-3333-4333-8333-333333333333',
    1
),
(
    '44444444-4444-4444-8444-444444444444',
    '99999999-9999-4999-8999-999999999999',
    2
);

-- 4. DEMO VEHICLE - SIX SEATS


INSERT INTO vehicle (
    vehicle_id,
    vehicle_code,
    license_plate,
    vehicle_type,
    capacity,
    layout_template,
    seat_layout_id,
    is_active
) VALUES (
    '66666666-6666-4666-8666-666666666666',
    'DEMO-VEHICLE-01',
    '47B-123.45',
    'Demo coach',
    6,
    'SEATED_6_1_DECK',
    'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
    TRUE
);


-- 5. PHYSICAL SEATS
-- Coordinates (deck, seat_row, seat_column)
-- are all zero-based.

INSERT INTO physical_seat (
    physical_seat_id, vehicle_id, seat_code,
    deck, seat_row, seat_column
) VALUES
(
    '77777777-7777-4777-8777-777777777777',
    '66666666-6666-4666-8666-666666666666',
    'A1', 0, 0, 0
),
(
    '88888888-8888-4888-8888-888888888888',
    '66666666-6666-4666-8666-666666666666',
    'A2', 0, 0, 2
),
(
    '77777777-7777-4777-8777-777777777778',
    '66666666-6666-4666-8666-666666666666',
    'B1', 0, 1, 0
),
(
    '77777777-7777-4777-8777-777777777779',
    '66666666-6666-4666-8666-666666666666',
    'B2', 0, 1, 2
),
(
    '77777777-7777-4777-8777-777777777780',
    '66666666-6666-4666-8666-666666666666',
    'C1', 0, 2, 0
),
(
    '77777777-7777-4777-8777-777777777781',
    '66666666-6666-4666-8666-666666666666',
    'C2', 0, 2, 2
);


-- =========================================================
-- 6. DEMO SCHEDULES
-- =========================================================
-- Two schedules for the same Route.
-- Times are local Vietnam time (Asia/Ho_Chi_Minh).
-- operating_days format is provisional pending review.

INSERT INTO schedule (
    schedule_id,
    route_id,
    operating_days,
    first_stop_departure_time
) VALUES
(
    'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa10',
    '44444444-4444-4444-8444-444444444444',
    'DAILY',
    TIME '09:00:00'
),
(
    'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa11',
    '44444444-4444-4444-8444-444444444444',
    'DAILY',
    TIME '22:30:00'
);


-- =========================================================
-- 7. DEMO TRIPS
-- =========================================================
-- Fixed test date D = 2026-11-20.
-- Both trips use the same Route and six-seat Vehicle.
-- Overnight Trip crosses into D+1 at intermediate Stops.

INSERT INTO trip (
    trip_id,
    route_id,
    schedule_id,
    vehicle_id,
    departure_date,
    status
) VALUES
(
    '11111111-1111-4111-8111-111111111111',
    '44444444-4444-4444-8444-444444444444',
    'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa10',
    '66666666-6666-4666-8666-666666666666',
    DATE '2026-11-20',
    'Scheduled'
),
(
    '11111111-1111-4111-8111-111111111112',
    '44444444-4444-4444-8444-444444444444',
    'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa11',
    '66666666-6666-4666-8666-666666666666',
    DATE '2026-11-20',
    'Scheduled'
);


-- =========================================================
-- 8. DEMO TRIP STOPS
-- =========================================================
-- All timestamps are UTC.
-- Stop order comes from ROUTE_STOP.stop_order.
-- A = index 0, B = index 1, C = index 2.

INSERT INTO trip_stop (
    trip_id,
    route_id,
    stop_id,
    planned_arrival_time,
    planned_departure_time
) VALUES

-- DAY TRIP: 2026-11-20
-- A: 09:00 Vietnam time
(
    '11111111-1111-4111-8111-111111111111',
    '44444444-4444-4444-8444-444444444444',
    '22222222-2222-4222-8222-222222222222',
    NULL,
    TIMESTAMPTZ '2026-11-20 02:00:00+00'
),

-- B: 11:00 Vietnam time
(
    '11111111-1111-4111-8111-111111111111',
    '44444444-4444-4444-8444-444444444444',
    '33333333-3333-4333-8333-333333333333',
    TIMESTAMPTZ '2026-11-20 04:00:00+00',
    TIMESTAMPTZ '2026-11-20 04:00:00+00'
),

-- C: 13:00 Vietnam time
(
    '11111111-1111-4111-8111-111111111111',
    '44444444-4444-4444-8444-444444444444',
    '99999999-9999-4999-8999-999999999999',
    TIMESTAMPTZ '2026-11-20 06:00:00+00',
    NULL
),

-- OVERNIGHT TRIP: 2026-11-20 to 2026-11-21
-- A: 22:30 Vietnam time
(
    '11111111-1111-4111-8111-111111111112',
    '44444444-4444-4444-8444-444444444444',
    '22222222-2222-4222-8222-222222222222',
    NULL,
    TIMESTAMPTZ '2026-11-20 15:30:00+00'
),

-- B: 00:30 Vietnam time on the next day
(
    '11111111-1111-4111-8111-111111111112',
    '44444444-4444-4444-8444-444444444444',
    '33333333-3333-4333-8333-333333333333',
    TIMESTAMPTZ '2026-11-20 17:30:00+00',
    TIMESTAMPTZ '2026-11-20 17:30:00+00'
),

-- C: 02:30 Vietnam time on the next day
(
    '11111111-1111-4111-8111-111111111112',
    '44444444-4444-4444-8444-444444444444',
    '99999999-9999-4999-8999-999999999999',
    TIMESTAMPTZ '2026-11-20 19:30:00+00',
    NULL
);


-- =========================================================
-- 9. DEMO LEG FARES
-- =========================================================
-- Each Trip has two consecutive legs:
-- A -> B = 100000 VND
-- B -> C = 150000 VND
-- A -> C = 250000 VND (calculated, not stored).

INSERT INTO leg_fare (
    trip_id,
    from_stop_id,
    to_stop_id,
    fare_amount_vnd
) VALUES
-- Day Trip: A -> B
(
    '11111111-1111-4111-8111-111111111111',
    '22222222-2222-4222-8222-222222222222',
    '33333333-3333-4333-8333-333333333333',
    100000
),
-- Day Trip: B -> C
(
    '11111111-1111-4111-8111-111111111111',
    '33333333-3333-4333-8333-333333333333',
    '99999999-9999-4999-8999-999999999999',
    150000
),
-- Overnight Trip: A -> B
(
    '11111111-1111-4111-8111-111111111112',
    '22222222-2222-4222-8222-222222222222',
    '33333333-3333-4333-8333-333333333333',
    100000
),
-- Overnight Trip: B -> C
(
    '11111111-1111-4111-8111-111111111112',
    '33333333-3333-4333-8333-333333333333',
    '99999999-9999-4999-8999-999999999999',
    150000
);