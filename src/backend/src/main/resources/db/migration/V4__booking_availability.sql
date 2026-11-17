CREATE TABLE booking (
    booking_id UUID NOT NULL,
    passenger_account_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    from_stop_id UUID NOT NULL,
    to_stop_id UUID NOT NULL,
    from_stop_index INTEGER NOT NULL,
    to_stop_index INTEGER NOT NULL,
    status TEXT NOT NULL,
    total_amount_vnd BIGINT NOT NULL,
    hold_created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT booking_pk PRIMARY KEY (booking_id),
    CONSTRAINT booking_passenger_fk FOREIGN KEY (passenger_account_id)
        REFERENCES identity_account (account_id),
    CONSTRAINT booking_trip_fk FOREIGN KEY (trip_id) REFERENCES trip (trip_id),
    CONSTRAINT booking_from_trip_stop_fk FOREIGN KEY (trip_id, from_stop_id)
        REFERENCES trip_stop (trip_id, stop_id),
    CONSTRAINT booking_to_trip_stop_fk FOREIGN KEY (trip_id, to_stop_id)
        REFERENCES trip_stop (trip_id, stop_id),
    CONSTRAINT booking_segment_valid CHECK (0 <= from_stop_index AND from_stop_index < to_stop_index),
    CONSTRAINT booking_status_valid CHECK (status IN ('Pending', 'Confirmed', 'Expired', 'Cancelled')),
    CONSTRAINT booking_total_nonnegative CHECK (total_amount_vnd >= 0),
    CONSTRAINT booking_hold_lifetime CHECK (
        isfinite(hold_created_at) AND isfinite(expires_at)
        AND expires_at = hold_created_at + INTERVAL '10 minutes'
    ),
    CONSTRAINT booking_trip_segment_uk UNIQUE (booking_id, trip_id, from_stop_index, to_stop_index),
    CONSTRAINT booking_expiry_uk UNIQUE (booking_id, expires_at)
);

CREATE TABLE booking_seat (
    booking_seat_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    physical_seat_id UUID NOT NULL,
    traveller_name TEXT NOT NULL,
    fare_amount_vnd BIGINT NOT NULL,
    CONSTRAINT booking_seat_pk PRIMARY KEY (booking_seat_id),
    CONSTRAINT booking_seat_booking_fk FOREIGN KEY (booking_id) REFERENCES booking (booking_id),
    CONSTRAINT booking_seat_physical_seat_fk FOREIGN KEY (physical_seat_id)
        REFERENCES physical_seat (physical_seat_id),
    CONSTRAINT booking_seat_traveller_nonblank CHECK (traveller_name ~ '[^[:space:]]'),
    CONSTRAINT booking_seat_fare_nonnegative CHECK (fare_amount_vnd >= 0),
    CONSTRAINT booking_seat_booking_physical_seat_uk UNIQUE (booking_id, physical_seat_id),
    CONSTRAINT booking_seat_parent_physical_seat_uk UNIQUE (booking_seat_id, booking_id, physical_seat_id)
);

CREATE TABLE seat_allocation (
    seat_allocation_id UUID NOT NULL,
    booking_seat_id UUID NOT NULL,
    -- Database-only parent key keeps every allocation on its Booking's Trip, Segment and expiry.
    booking_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    physical_seat_id UUID NOT NULL,
    from_stop_index INTEGER NOT NULL,
    to_stop_index INTEGER NOT NULL,
    kind TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    CONSTRAINT seat_allocation_pk PRIMARY KEY (seat_allocation_id),
    CONSTRAINT seat_allocation_booking_seat_fk FOREIGN KEY (booking_seat_id, booking_id, physical_seat_id)
        REFERENCES booking_seat (booking_seat_id, booking_id, physical_seat_id),
    CONSTRAINT seat_allocation_booking_trip_segment_fk
        FOREIGN KEY (booking_id, trip_id, from_stop_index, to_stop_index)
        REFERENCES booking (booking_id, trip_id, from_stop_index, to_stop_index),
    CONSTRAINT seat_allocation_booking_expiry_fk FOREIGN KEY (booking_id, expires_at)
        REFERENCES booking (booking_id, expires_at),
    CONSTRAINT seat_allocation_segment_valid CHECK (0 <= from_stop_index AND from_stop_index < to_stop_index),
    CONSTRAINT seat_allocation_kind_valid CHECK (kind IN ('Hold', 'Confirmed')),
    -- Nullable Confirmed expiry must not bypass the independent parent/seat/Trip/Segment FKs.
    CONSTRAINT seat_allocation_hold_expiry_required CHECK (kind <> 'Hold' OR expires_at IS NOT NULL)
);

CREATE INDEX seat_allocation_candidate_idx
    ON seat_allocation (trip_id, physical_seat_id, from_stop_index, to_stop_index)
    WHERE released_at IS NULL;

CREATE INDEX booking_pending_expiry_idx ON booking (expires_at) WHERE status = 'Pending';
