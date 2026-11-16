
package vn.edu.team11.coach.trip.infrastructure;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.trip.contract.TripQueryErrorCode;
import vn.edu.team11.coach.trip.contract.TripQueryException;
import vn.edu.team11.coach.trip.contract.TripQueryPort;
import vn.edu.team11.coach.trip.contract.TripSeatFacts;

@SpringBootTest
@Transactional
class PostgresTripQueryAdapterIT {

        @Autowired
        private TripQueryPort tripQueryPort;
        @Autowired
        private JdbcTemplate jdbc;
        @Test
        void missingTripReturnsTypedNotFoundError() {
                UUID missingTripId = UUID.randomUUID();

                assertThatThrownBy(() ->
                        tripQueryPort.getSeatAvailabilityFacts(
                                missingTripId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                Instant.parse("2026-11-20T00:00:00Z")
                        )
                )
                        .isInstanceOf(TripQueryException.class)
                        .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.TRIP_NOT_FOUND
                                );
                        });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void validTripReturnsVerifiedSeatFacts() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID fromStopId = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID toStopId = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        Instant asOf = Instant.parse("2026-11-20T00:00:00Z");

        TripSeatFacts facts = tripQueryPort.getSeatAvailabilityFacts(
                tripId,
                fromStopId,
                toStopId,
                asOf
        );

        assertThat(facts.tripId()).isEqualTo(tripId);
        assertThat(facts.fromStopId()).isEqualTo(fromStopId);
        assertThat(facts.toStopId()).isEqualTo(toStopId);

        assertThat(facts.fromStopIndex()).isZero();
        assertThat(facts.toStopIndex()).isEqualTo(2);

        assertThat(facts.bookingClosesAt()).isEqualTo(
                Instant.parse("2026-11-20T01:30:00Z")
        );

        assertThat(facts.asOf()).isEqualTo(asOf);

        assertThat(facts.seatLayout().id()).isEqualTo(
                UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
        );

        assertThat(facts.seatLayout().seats()).hasSize(6);

        assertThat(
                facts.seatLayout().seats().stream()
                        .map(seat -> seat.label())
                        .toList()
        ).containsExactly(
                "A1", "A2", "B1", "B2", "C1", "C2"
        );
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void tripAtExactCutoffReturnsNotBookable() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID fromStopId = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID toStopId = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        Instant exactCutoff = Instant.parse(
                "2026-11-20T01:30:00Z"
        );

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        fromStopId,
                        toStopId,
                        exactCutoff
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.TRIP_NOT_BOOKABLE
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void missingLegFareReturnsEssentialDataIncomplete() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopB = UUID.fromString(
                "33333333-3333-4333-8333-333333333333"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        jdbc.update(
                """
                DELETE FROM leg_fare
                WHERE trip_id = ?
                AND from_stop_id = ?
                AND to_stop_id = ?
                """,
                tripId,
                stopB,
                stopC
        );

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        stopC,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void missingPhysicalSeatReturnsEssentialDataIncomplete() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        UUID missingSeatId = UUID.fromString(
                "77777777-7777-4777-8777-777777777777"
        );

        int deleted = jdbc.update(
                """
                DELETE FROM physical_seat
                WHERE physical_seat_id = ?
                """,
                missingSeatId
        );

        assertThat(deleted).isEqualTo(1);

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        stopC,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void missingTripStopReturnsEssentialDataIncomplete() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopB = UUID.fromString(
                "33333333-3333-4333-8333-333333333333"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        jdbc.update(
                """
                DELETE FROM leg_fare
                WHERE trip_id = ?
                AND (from_stop_id = ? OR to_stop_id = ?)
                """,
                tripId,
                stopC,
                stopC
        );

        jdbc.update(
                """
                DELETE FROM trip_stop
                WHERE trip_id = ?
                AND stop_id = ?
                """,
                tripId,
                stopC
        );

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        stopB,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void nonContiguousStopOrderReturnsEssentialDataIncomplete() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopB = UUID.fromString(
                "33333333-3333-4333-8333-333333333333"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        UUID routeId = UUID.fromString(
                "44444444-4444-4444-8444-444444444444"
        );

        int updated = jdbc.update(
                """
                UPDATE route_stop
                SET stop_order = 3
                WHERE route_id = ?
                AND stop_id = ?
                """,
                routeId,
                stopB
        );

        assertThat(updated).isEqualTo(1);

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        stopC,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void nonChronologicalTripStopReturnsEssentialDataIncomplete() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopB = UUID.fromString(
                "33333333-3333-4333-8333-333333333333"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        int updated = jdbc.update(
                """
                UPDATE trip_stop
                SET planned_arrival_time =
                        TIMESTAMPTZ '2026-11-20 01:00:00+00',
                        planned_departure_time =
                        TIMESTAMPTZ '2026-11-20 01:00:00+00'
                WHERE trip_id = ?
                AND stop_id = ?
                """,
                tripId,
                stopB
        );

        assertThat(updated).isEqualTo(1);

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        stopC,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void unknownStopReturnsStopNotServed() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID unknownStop = UUID.fromString(
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        );

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        unknownStop,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.STOP_NOT_SERVED
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void reversedSegmentReturnsInvalidSegment() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopC,
                        stopA,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.INVALID_SEGMENT
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void departedTripReturnsTripNotBookable() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        int updated = jdbc.update(
                """
                UPDATE trip
                SET status = 'Departed'
                WHERE trip_id = ?
                """,
                tripId
        );

        assertThat(updated).isEqualTo(1);

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        stopC,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.TRIP_NOT_BOOKABLE
                                );
                });
        }

        @Test
        @Sql("/fixtures/trip_demo.sql")
        void mismatchedRouteStopReturnsEssentialDataIncomplete() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID routeId = UUID.fromString(
                "44444444-4444-4444-8444-444444444444"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopB = UUID.fromString(
                "33333333-3333-4333-8333-333333333333"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        // Change Route order without changing TripStop.
        // Counts and indices remain valid, but Stop IDs no longer match.
        int movedC = jdbc.update(
                """
                UPDATE route_stop
                SET stop_order = 3
                WHERE route_id = ? AND stop_id = ?
                """,
                routeId, stopC
        );

        int movedB = jdbc.update(
                """
                UPDATE route_stop
                SET stop_order = 2
                WHERE route_id = ? AND stop_id = ?
                """,
                routeId, stopB
        );

        int movedCAgain = jdbc.update(
                """
                UPDATE route_stop
                SET stop_order = 1
                WHERE route_id = ? AND stop_id = ?
                """,
                routeId, stopC
        );

        assertThat(movedC).isEqualTo(1);
        assertThat(movedB).isEqualTo(1);
        assertThat(movedCAgain).isEqualTo(1);

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        stopB,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(TripQueryException.class)
                .satisfies(error -> {
                        TripQueryException exception =
                                (TripQueryException) error;

                        assertThat(exception.getCode())
                                .isEqualTo(
                                        TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE
                                );
                });
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void callingAdapterWithoutTransactionIsRejected() {
        UUID tripId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID stopA = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );

        UUID stopC = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        assertThatThrownBy(() ->
                tripQueryPort.getSeatAvailabilityFacts(
                        tripId,
                        stopA,
                        stopC,
                        Instant.parse("2026-11-20T00:00:00Z")
                )
        )
                .isInstanceOf(
                        org.springframework.transaction.IllegalTransactionStateException.class
                );
        }

}
