package vn.edu.team11.coach.booking.api;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;

import vn.edu.team11.coach.booking.application.SeatAvailabilityApplicationService;
import vn.edu.team11.coach.booking.domain.BookingStatus;
import vn.edu.team11.coach.booking.domain.SeatAllocation;
import vn.edu.team11.coach.booking.domain.Segment;
import vn.edu.team11.coach.booking.infrastructure.BookingRepository;
import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.contract.SeatLayout;
import vn.edu.team11.coach.trip.contract.TripQueryErrorCode;
import vn.edu.team11.coach.trip.contract.TripQueryException;
import vn.edu.team11.coach.trip.contract.TripQueryPort;
import vn.edu.team11.coach.trip.contract.TripSeatFacts;
import vn.edu.team11.coach.trip.contract.TripStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Standalone MVC deliberately excludes security filters; public runtime integration remains pending.
class SeatAvailabilityControllerTests {
    private static final UUID TRIP = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID FROM = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID TO = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID SEAT = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final Instant NOW = Instant.parse("2026-11-19T00:08:00Z");
    private static final String PATH = "/api/v1/trips/" + TRIP + "/seat-availability";
    private static final String PRIVATE = "SQL password traveller Booking private";
    private final TripQueryPort port = mock(TripQueryPort.class);
    private final BookingRepository repository = mock(BookingRepository.class);
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        beans.registerSingleton("tripQueryPort", port);
        var service = new SeatAvailabilityApplicationService(beans.getBeanProvider(TripQueryPort.class), repository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new SeatAvailabilityController(service))
                .setControllerAdvice(new SeatAvailabilityExceptionHandler()).build();
        var layout = new SeatLayout(UUID.fromString("55555555-5555-4555-8555-555555555555"),
                List.of(new LayoutSeat(SEAT, "A1", 0, 1, 2)));
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenReturn(new TripSeatFacts(TRIP, FROM, TO,
                0, 2, TripStatus.Scheduled, NOW.plusSeconds(600), layout, NOW));
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenReturn(List.of());
    }

    @Test
    void returnsExactPublicSchemaWithNoStoreAndExplicitNullExpiry() throws Exception {
        var response = mvc.perform(request()).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tripId").value(TRIP.toString()))
                .andExpect(jsonPath("$.fromStopId").value(FROM.toString()))
                .andExpect(jsonPath("$.toStopId").value(TO.toString()))
                .andExpect(jsonPath("$.serverTime").value("2026-11-19T00:08:00Z"))
                .andExpect(jsonPath("$.bookingClosesAt").value("2026-11-19T00:18:00Z"))
                .andExpect(jsonPath("$.layout.id").value("55555555-5555-4555-8555-555555555555"))
                .andExpect(jsonPath("$.layout.seats[0].physicalSeatId").value(SEAT.toString()))
                .andExpect(jsonPath("$.layout.seats[0].label").value("A1"))
                .andExpect(jsonPath("$.layout.seats[0].deck").value(0))
                .andExpect(jsonPath("$.layout.seats[0].row").value(1))
                .andExpect(jsonPath("$.layout.seats[0].column").value(2))
                .andExpect(jsonPath("$.seats[0].physicalSeatId").value(SEAT.toString()))
                .andExpect(jsonPath("$.seats[0].status").value("FREE"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = new ObjectMapper().readTree(response);
        assertThat(json.properties()).extracting(java.util.Map.Entry::getKey)
                .containsExactlyInAnyOrder("tripId", "fromStopId", "toStopId", "serverTime", "bookingClosesAt", "layout", "seats");
        assertThat(json.path("seats").get(0).has("expiresAt")).isTrue();
        assertThat(json.path("seats").get(0).get("expiresAt").isNull()).isTrue();
        assertThat(json.path("seats").get(0).properties()).extracting(java.util.Map.Entry::getKey)
                .containsExactlyInAnyOrder("physicalSeatId", "status", "expiresAt");
        verify(repository, never()).expireDueBookings(any());
    }

    @Test
    void allUnavailableRemainsSuccessfulWithConfirmedNullExpiry() throws Exception {
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenReturn(List.of(new SeatAllocation(TRIP, SEAT,
                new Segment(0, 2), BookingStatus.Confirmed, SeatAllocation.Kind.Confirmed, NOW.minusSeconds(30), null)));
        mvc.perform(request()).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.seats[0].status").value("UNAVAILABLE"));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsMalformedMissingAndDuplicateScalarsBeforeCallingPort(MockHttpServletRequestBuilder request, String field) throws Exception {
        mvc.perform(request).andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
        verifyNoInteractions(port, repository);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(get(PATH).param("toStopId", TO.toString()), "fromStopId"),
                Arguments.of(get(PATH).param("fromStopId", FROM.toString()), "toStopId"),
                Arguments.of(request().param("fromStopId", FROM.toString()), "fromStopId"),
                Arguments.of(request().param("toStopId", TO.toString()), "toStopId"),
                Arguments.of(get(PATH).param("fromStopId", "").param("toStopId", TO.toString()), "fromStopId"),
                Arguments.of(get(PATH).param("fromStopId", "1-1-1-1-1").param("toStopId", TO.toString()), "fromStopId"),
                Arguments.of(get(PATH).param("fromStopId", FROM.toString()).param("toStopId", " " + TO), "toStopId"),
                Arguments.of(get("/api/v1/trips/1-1-1-1-1/seat-availability").param("fromStopId", FROM.toString()).param("toStopId", TO.toString()), "tripId"),
                Arguments.of(get(PATH).param("fromStopId", FROM.toString()).param("toStopId", "invalid"), "toStopId"));
    }

    @ParameterizedTest
    @MethodSource("caseInsensitiveIds")
    void acceptsUpperAndMixedCaseCanonicalUuids(String field, String value) throws Exception {
        UUID trip = field.equals("tripId") ? UUID.fromString(value) : TRIP;
        UUID from = field.equals("fromStopId") ? UUID.fromString(value) : FROM;
        UUID to = field.equals("toStopId") ? UUID.fromString(value) : TO;
        var layout = new SeatLayout(SEAT, List.of(new LayoutSeat(SEAT, "A1", 0, 1, 2)));
        when(port.getSeatAvailabilityFacts(trip, from, to, NOW)).thenReturn(new TripSeatFacts(trip, from, to,
                0, 2, TripStatus.Scheduled, NOW.plusSeconds(600), layout, NOW));
        when(repository.findCandidates(trip, new Segment(0, 2))).thenReturn(List.of());

        mvc.perform(get("/api/v1/trips/" + (field.equals("tripId") ? value : TRIP) + "/seat-availability")
                        .param("fromStopId", field.equals("fromStopId") ? value : FROM.toString())
                        .param("toStopId", field.equals("toStopId") ? value : TO.toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tripId").value(trip.toString()))
                .andExpect(jsonPath("$.fromStopId").value(from.toString()))
                .andExpect(jsonPath("$.toStopId").value(to.toString()));
        verify(port).getSeatAvailabilityFacts(trip, from, to, NOW);
    }

    static Stream<Arguments> caseInsensitiveIds() {
        return Stream.of("tripId", "fromStopId", "toStopId").flatMap(field -> Stream.of(
                Arguments.of(field, "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"),
                Arguments.of(field, "aAaAaAaA-AaAa-4aAa-8AaA-aAaAaAaAaAaA")));
    }

    @ParameterizedTest
    @MethodSource("typedFailures")
    void mapsEveryTypedTripErrorWithoutLeakingProviderDetails(TripQueryErrorCode code, int status, String publicCode) throws Exception {
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenThrow(new TripQueryException(code, PRIVATE));
        assertError(status, publicCode);
    }

    static Stream<Arguments> typedFailures() {
        return Stream.of(Arguments.of(TripQueryErrorCode.TRIP_NOT_FOUND, 404, "TRIP_NOT_FOUND"),
                Arguments.of(TripQueryErrorCode.STOP_NOT_SERVED, 422, "STOP_NOT_SERVED"),
                Arguments.of(TripQueryErrorCode.INVALID_SEGMENT, 422, "INVALID_SEGMENT"),
                Arguments.of(TripQueryErrorCode.TRIP_NOT_BOOKABLE, 409, "TRIP_NOT_BOOKABLE"),
                Arguments.of(TripQueryErrorCode.ESSENTIAL_DATA_INCOMPLETE, 409, "ESSENTIAL_DATA_INCOMPLETE"),
                Arguments.of(TripQueryErrorCode.TRIP_DATA_UNAVAILABLE, 503, "AVAILABILITY_UNAVAILABLE"));
    }

    @Test
    void missingRealPortIsUnavailable() throws Exception {
        beans.destroySingleton("tripQueryPort");
        assertError(503, "AVAILABILITY_UNAVAILABLE");
    }

    @Test
    void failedReadIsUnavailable() throws Exception {
        when(repository.findCandidates(TRIP, new Segment(0, 2))).thenThrow(new DataAccessResourceFailureException(PRIVATE));
        assertError(503, "AVAILABILITY_UNAVAILABLE");
    }

    @Test
    void inabilityToOpenSnapshotIsUnavailable() throws Exception {
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenThrow(new CannotCreateTransactionException(PRIVATE));
        assertError(503, "AVAILABILITY_UNAVAILABLE");
    }

    @Test
    void unverifiedReadIsUnavailable() throws Exception {
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenReturn(null);
        assertError(503, "AVAILABILITY_UNAVAILABLE");
    }

    @Test
    void unexpectedFailureIsSanitizedInternalError() throws Exception {
        when(port.getSeatAvailabilityFacts(TRIP, FROM, TO, NOW)).thenThrow(new IllegalStateException(PRIVATE));
        assertError(500, "INTERNAL_ERROR");
    }

    private void assertError(int status, String code) throws Exception {
        String response = mvc.perform(request()).andExpect(status().is(status))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value(code)).andExpect(jsonPath("$.fieldErrors").isArray())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = new ObjectMapper().readTree(response);
        Set<String> fields = json.properties().stream().map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
        assertThat(fields).containsExactlyInAnyOrder("code", "message", "fieldErrors");
        assertThat(json.path("message").asText()).isNotBlank();
        assertThat(response).doesNotContain("SQL", "password", "traveller", "Booking", "private", "stackTrace", "exception");
    }

    private static MockHttpServletRequestBuilder request() {
        return get(PATH).param("fromStopId", FROM.toString()).param("toStopId", TO.toString());
    }
}
