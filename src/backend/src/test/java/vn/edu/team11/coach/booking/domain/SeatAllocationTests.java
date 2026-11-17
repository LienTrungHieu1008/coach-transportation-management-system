package vn.edu.team11.coach.booking.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SeatAllocationTests {
    private static final UUID TRIP_ID = new UUID(0, 1);
    private static final UUID SEAT_ID = new UUID(0, 2);
    private static final Segment SEGMENT = new Segment(0, 2);
    private static final Instant AS_OF = Instant.parse("2026-11-15T00:00:00Z");
    private static final Instant EXPIRY = Instant.parse("2026-11-15T00:10:00Z");

    @ParameterizedTest
    @CsvSource({
            "Pending, Hold, false, true",
            "Pending, Confirmed, false, false",
            "Confirmed, Hold, false, false",
            "Confirmed, Confirmed, false, true",
            "Expired, Hold, false, false",
            "Expired, Confirmed, false, false",
            "Cancelled, Hold, false, false",
            "Cancelled, Confirmed, false, false",
            "Pending, Hold, true, false",
            "Pending, Confirmed, true, false",
            "Confirmed, Hold, true, false",
            "Confirmed, Confirmed, true, false",
            "Expired, Hold, true, false",
            "Expired, Confirmed, true, false",
            "Cancelled, Hold, true, false",
            "Cancelled, Confirmed, true, false"
    })
    void onlyMatchingUnreleasedBookingAndAllocationStatesAreEffective(
            BookingStatus status, SeatAllocation.Kind kind, boolean released, boolean expected) {
        var allocation = new SeatAllocation(TRIP_ID, SEAT_ID, SEGMENT, status, kind, EXPIRY,
                released ? AS_OF.minusSeconds(1) : null);

        assertThat(allocation.isEffective(AS_OF)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-11-15T00:09:59.999999999Z, true",
            "2026-11-15T00:10:00Z, false",
            "2026-11-15T00:10:00.000000001Z, false"
    })
    void holdStopsBlockingExactlyAtServerExpiry(String asOf, boolean expected) {
        var hold = new SeatAllocation(TRIP_ID, SEAT_ID, SEGMENT, BookingStatus.Pending,
                SeatAllocation.Kind.Hold, EXPIRY, null);

        assertThat(hold.isEffective(Instant.parse(asOf))).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"2026-11-14T00:00:00Z", "2026-11-16T00:00:00Z"})
    void confirmedAllocationDoesNotExpireWithItsFormerHold(String expiry) {
        var confirmed = new SeatAllocation(TRIP_ID, SEAT_ID, SEGMENT, BookingStatus.Confirmed,
                SeatAllocation.Kind.Confirmed, expiry == null ? null : Instant.parse(expiry), null);

        assertThat(confirmed.isEffective(AS_OF)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"tripId", "physicalSeatId", "segment", "bookingStatus", "kind", "expiresAt"})
    void rejectsMissingAllocationData(String missing) {
        assertThatNullPointerException().isThrownBy(() -> new SeatAllocation(
                missing.equals("tripId") ? null : TRIP_ID,
                missing.equals("physicalSeatId") ? null : SEAT_ID,
                missing.equals("segment") ? null : SEGMENT,
                missing.equals("bookingStatus") ? null : BookingStatus.Pending,
                missing.equals("kind") ? null : SeatAllocation.Kind.Hold,
                missing.equals("expiresAt") ? null : EXPIRY, null));
    }

    @Test
    void rejectsMissingAuthoritativeTime() {
        var confirmed = new SeatAllocation(TRIP_ID, SEAT_ID, SEGMENT, BookingStatus.Confirmed,
                SeatAllocation.Kind.Confirmed, null, null);

        assertThatNullPointerException().isThrownBy(() -> confirmed.isEffective(null));
    }
}
