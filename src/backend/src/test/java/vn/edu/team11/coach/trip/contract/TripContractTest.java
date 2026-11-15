
package vn.edu.team11.coach.trip.contract;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TripContractTest {

    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID FROM_STOP_ID = UUID.randomUUID();
    private static final UUID TO_STOP_ID = UUID.randomUUID();
    private static final UUID LAYOUT_ID = UUID.randomUUID();

    private LayoutSeat seat(int row, int column) {
        return new LayoutSeat(
                UUID.randomUUID(),
                "A" + row + column,
                0,
                row,
                column
        );
    }

    @Test
    void layoutRejectsEmptySeats() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SeatLayout(LAYOUT_ID, List.of())
        );
    }

    @Test
    void layoutRejectsDuplicateSeatIds() {
        UUID id = UUID.randomUUID();

        LayoutSeat first = new LayoutSeat(id, "A1", 0, 0, 0);
        LayoutSeat second = new LayoutSeat(id, "A2", 0, 0, 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> new SeatLayout(LAYOUT_ID, List.of(first, second))
        );
    }

    @Test
    void layoutRejectsDuplicatePositions() {
        LayoutSeat first = seat(0, 0);
        LayoutSeat second = seat(0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> new SeatLayout(LAYOUT_ID, List.of(first, second))
        );
    }

    @Test
    void layoutSortsSeatsAndCopiesList() {
        LayoutSeat later = seat(2, 1);
        LayoutSeat earlier = seat(0, 0);

        List<LayoutSeat> source = new ArrayList<>();
        source.add(later);
        source.add(earlier);

        SeatLayout layout = new SeatLayout(LAYOUT_ID, source);

        source.clear();

        assertEquals(2, layout.seats().size());
        assertEquals(earlier, layout.seats().get(0));
        assertEquals(later, layout.seats().get(1));

        assertThrows(
                UnsupportedOperationException.class,
                () -> layout.seats().clear()
        );
    }

    @Test
    void layoutSeatRejectsInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LayoutSeat(
                        UUID.randomUUID(), "A1", -1, 0, 0
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new LayoutSeat(
                        UUID.randomUUID(), " ", 0, 0, 0
                )
        );
    }

    @Test
    void tripFactsRejectInvalidSegmentIndices() {
        SeatLayout layout = new SeatLayout(
                LAYOUT_ID,
                List.of(seat(0, 0))
        );

        Instant now = Instant.parse("2026-11-15T07:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new TripSeatFacts(
                        TRIP_ID,
                        FROM_STOP_ID,
                        TO_STOP_ID,
                        2,
                        1,
                        TripStatus.Scheduled,
                        now.plusSeconds(1800),
                        layout,
                        now
                )
        );
    }

    @Test
    void exceptionExposesTypedErrorCode() {
        TripQueryException exception = new TripQueryException(
                TripQueryErrorCode.TRIP_NOT_FOUND,
                "Trip not found"
        );

        assertEquals(
                TripQueryErrorCode.TRIP_NOT_FOUND,
                exception.getCode()
        );
    }
}
