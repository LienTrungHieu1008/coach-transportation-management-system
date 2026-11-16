-- P1 Trip & Operations read model for PA4.
-- Owner: Do Minh Phuong - Team 11.
--
-- Depends on Flyway V2__identity_account.sql.
-- Schema only; demo data belongs in separate fixtures.
--
-- Supports UC-P04 (Search Trips), UC-P05 (Trip Details)
-- and Trip facts required by UC-P07 (Seat Availability).
--
-- Do not modify V1, V2 or the submitted PA3 artifacts.

-- =========================================================
-- 1. ROUTE
-- =========================================================
-- A Route describes an ordered intercity bus route.

CREATE TABLE route (
    route_id UUID NOT NULL,
    route_code TEXT NOT NULL,
    route_name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT route_pk PRIMARY KEY (route_id),
    CONSTRAINT route_code_uk UNIQUE (route_code),

    CONSTRAINT route_code_nonblank CHECK (
        char_length(btrim(route_code)) > 0
    ),
    CONSTRAINT route_name_nonblank CHECK (
        char_length(btrim(route_name)) > 0
    )
);

-- =========================================================
-- 2. STOP
-- =========================================================
-- Stop is a shared catalogue of boarding/alighting points.

CREATE TABLE stop (
    stop_id UUID NOT NULL,
    stop_name TEXT NOT NULL,
    address TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT stop_pk PRIMARY KEY (stop_id),

    CONSTRAINT stop_name_nonblank CHECK (
        char_length(btrim(stop_name)) > 0
    ),
    CONSTRAINT stop_address_nonblank CHECK (
        char_length(btrim(address)) > 0
    )
);

-- =========================================================
-- 3. ROUTE_STOP
-- =========================================================
-- Defines which Stops belong to a Route and their order.
-- stop_order is 0-based for PA4.

CREATE TABLE route_stop (
    route_id UUID NOT NULL,
    stop_id UUID NOT NULL,
    stop_order INTEGER NOT NULL,

    CONSTRAINT route_stop_pk
        PRIMARY KEY (route_id, stop_id),

    CONSTRAINT route_stop_route_fk
        FOREIGN KEY (route_id)
        REFERENCES route (route_id),

    CONSTRAINT route_stop_stop_fk
        FOREIGN KEY (stop_id)
        REFERENCES stop (stop_id),

    CONSTRAINT route_stop_order_uk
        UNIQUE (route_id, stop_order),

    CONSTRAINT route_stop_order_nonnegative
        CHECK (stop_order >= 0)
);


-- =========================================================
-- 4. VEHICLE
-- =========================================================
-- A Vehicle is assigned to a Trip and owns physical seats.

CREATE TABLE vehicle (
    vehicle_id UUID NOT NULL,
    vehicle_code TEXT NOT NULL,
    vehicle_type TEXT NOT NULL,
    capacity INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT vehicle_pk
        PRIMARY KEY (vehicle_id),

    CONSTRAINT vehicle_code_uk
        UNIQUE (vehicle_code),

    CONSTRAINT vehicle_code_nonblank
        CHECK (char_length(btrim(vehicle_code)) > 0),

    CONSTRAINT vehicle_type_nonblank
        CHECK (char_length(btrim(vehicle_type)) > 0),

    CONSTRAINT vehicle_capacity_positive
        CHECK (capacity > 0)
);

-- =========================================================
-- 5. PHYSICAL_SEAT
-- =========================================================
-- Every seat has a stable UUID and belongs to one Vehicle.
-- Coordinates are 0-based as agreed for PA4.
-- The seat identity must not be derived from its position.

CREATE TABLE physical_seat (
    physical_seat_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    seat_code TEXT NOT NULL,
    deck INTEGER NOT NULL,
    seat_row INTEGER NOT NULL,
    seat_column INTEGER NOT NULL,

    CONSTRAINT physical_seat_pk
        PRIMARY KEY (physical_seat_id),

    CONSTRAINT physical_seat_vehicle_fk
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicle (vehicle_id),

    CONSTRAINT physical_seat_code_uk
        UNIQUE (vehicle_id, seat_code),

    CONSTRAINT physical_seat_position_uk
        UNIQUE (vehicle_id, deck, seat_row, seat_column),

    CONSTRAINT physical_seat_code_nonblank
        CHECK (char_length(btrim(seat_code)) > 0),

    CONSTRAINT physical_seat_position_nonnegative
        CHECK (
            deck >= 0
            AND seat_row >= 0
            AND seat_column >= 0
        )
);


-- =========================================================
-- 6. SCHEDULE
-- =========================================================
-- A Schedule is a recurring template for a Route.
-- Trip instances remain independent of Schedule lifecycle.

CREATE TABLE schedule (
    schedule_id UUID NOT NULL,
    route_id UUID NOT NULL,
    operating_days TEXT NOT NULL,
    first_stop_departure_time TIME NOT NULL,

    CONSTRAINT schedule_pk
        PRIMARY KEY (schedule_id),

    CONSTRAINT schedule_id_route_uk
        UNIQUE (schedule_id, route_id),

    CONSTRAINT schedule_route_fk
        FOREIGN KEY (route_id)
        REFERENCES route (route_id),

    CONSTRAINT schedule_operating_days_nonblank
        CHECK (char_length(btrim(operating_days)) > 0)
);


-- =========================================================
-- 7. TRIP
-- =========================================================
-- A Trip is one concrete service of a Route.
-- Planned stop times are stored in TRIP_STOP.

CREATE TABLE trip (
    trip_id UUID NOT NULL,
    route_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    departure_date DATE NOT NULL,
    status TEXT NOT NULL,

    CONSTRAINT trip_pk
        PRIMARY KEY (trip_id),

    CONSTRAINT trip_id_route_uk
        UNIQUE (trip_id, route_id),

    CONSTRAINT trip_route_fk
        FOREIGN KEY (route_id)
        REFERENCES route (route_id),

    CONSTRAINT trip_schedule_route_fk
        FOREIGN KEY (schedule_id, route_id)
        REFERENCES schedule (schedule_id, route_id),

    CONSTRAINT trip_vehicle_fk
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicle (vehicle_id),

    CONSTRAINT trip_status_valid
        CHECK (
            status IN (
                'Scheduled',
                'Departed',
                'Completed',
                'Cancelled'
            )
        )
);


-- =========================================================
-- 8. TRIP_STOP
-- =========================================================
-- Planned arrival/departure times for each Stop of a Trip.
-- Stop ordering is derived from ROUTE_STOP.stop_order.
-- route_id is included to enforce Route membership by FK.

CREATE TABLE trip_stop (
    trip_id UUID NOT NULL,
    route_id UUID NOT NULL,
    stop_id UUID NOT NULL,

    planned_arrival_time TIMESTAMPTZ,
    planned_departure_time TIMESTAMPTZ,

    CONSTRAINT trip_stop_pk
        PRIMARY KEY (trip_id, stop_id),

    CONSTRAINT trip_stop_trip_route_fk
        FOREIGN KEY (trip_id, route_id)
        REFERENCES trip (trip_id, route_id),

    CONSTRAINT trip_stop_route_stop_fk
        FOREIGN KEY (route_id, stop_id)
        REFERENCES route_stop (route_id, stop_id),

    CONSTRAINT trip_stop_time_required
        CHECK (
            planned_arrival_time IS NOT NULL
            OR planned_departure_time IS NOT NULL
        ),

    CONSTRAINT trip_stop_time_valid
        CHECK (
            planned_arrival_time IS NULL
            OR planned_departure_time IS NULL
            OR planned_arrival_time <= planned_departure_time
        )
);

-- =========================================================
-- 9. LEG_FARE
-- =========================================================
-- Stores the fare for one consecutive leg of a Trip.
-- Segment fare is the sum of consecutive leg fares.
-- Fare amounts are nonnegative integer VND.
--
-- Both endpoints must belong to the same Trip.
-- Stop adjacency must be validated against
-- ROUTE_STOP.stop_order before a Trip becomes sellable.

CREATE TABLE leg_fare (
    trip_id UUID NOT NULL,
    from_stop_id UUID NOT NULL,
    to_stop_id UUID NOT NULL,
    fare_amount_vnd BIGINT NOT NULL,

    CONSTRAINT leg_fare_pk
        PRIMARY KEY (
            trip_id,
            from_stop_id,
            to_stop_id
        ),

    CONSTRAINT leg_fare_from_trip_stop_fk
        FOREIGN KEY (trip_id, from_stop_id)
        REFERENCES trip_stop (trip_id, stop_id),

    CONSTRAINT leg_fare_to_trip_stop_fk
        FOREIGN KEY (trip_id, to_stop_id)
        REFERENCES trip_stop (trip_id, stop_id),

    CONSTRAINT leg_fare_distinct_stops
        CHECK (from_stop_id <> to_stop_id),

    CONSTRAINT leg_fare_amount_nonnegative
        CHECK (fare_amount_vnd >= 0)
);
