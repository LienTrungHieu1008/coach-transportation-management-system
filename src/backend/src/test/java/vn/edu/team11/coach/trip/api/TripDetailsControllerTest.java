package vn.edu.team11.coach.trip.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;
import vn.edu.team11.coach.identity.infrastructure.SecurityConfiguration;
import vn.edu.team11.coach.trip.application.TripDetailsException;
import vn.edu.team11.coach.trip.application.TripDetailsService;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;
import static vn.edu.team11.coach.trip.TripDetailsTestData.complete;

@WebMvcTest(TripDetailsController.class)
@Import({SecurityConfiguration.class, TripDetailsRequestParser.class, TripDetailsExceptionHandler.class})
class TripDetailsControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private TripDetailsService service;
    private static final String PATH = "/api/v1/trips/" + DAY;
    @Test
    void exactResponseContainsAllRequiredKeysIncludingNullsAndNoExtraFields() throws Exception {
        when(service.details(any())).thenReturn(new TripDetailsPolicy().evaluate(complete(), A, C, NOW));
        mvc.perform(get(PATH).param("fromStopId", A.toString()).param("toStopId", C.toString()).with(user("passenger")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"tripId":"11111111-1111-4111-8111-111111111111","status":"Scheduled",
                         "route":{"id":"44444444-4444-4444-8444-444444444444","name":"Route"},
                         "vehicle":{"id":"66666666-6666-4666-8666-666666666666","vehicleType":"Coach",
                                    "seatLayoutId":"bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"},
                         "stops":[
                           {"stop":{"id":"22222222-2222-4222-8222-222222222222","name":"A"},"index":0,
                            "plannedArrivalTime":null,"plannedDepartureTime":"2026-11-20T02:00:00Z"},
                           {"stop":{"id":"33333333-3333-4333-8333-333333333333","name":"B"},"index":1,
                            "plannedArrivalTime":"2026-11-20T04:00:00Z","plannedDepartureTime":"2026-11-20T04:00:00Z"},
                           {"stop":{"id":"99999999-9999-4999-8999-999999999999","name":"C"},"index":2,
                            "plannedArrivalTime":"2026-11-20T06:00:00Z","plannedDepartureTime":null}],
                         "legFares":[
                           {"fromStopId":"22222222-2222-4222-8222-222222222222","toStopId":"33333333-3333-4333-8333-333333333333","fareAmountVnd":100000},
                           {"fromStopId":"33333333-3333-4333-8333-333333333333","toStopId":"99999999-9999-4999-8999-999999999999","fareAmountVnd":150000}],
                         "description":null,"selectedSegment":{
                           "fromStopId":"22222222-2222-4222-8222-222222222222","toStopId":"99999999-9999-4999-8999-999999999999",
                           "boardingTime":"2026-11-20T02:00:00Z","alightingTime":"2026-11-20T06:00:00Z","fareAmountVnd":250000},
                         "bookingClosesAt":"2026-11-20T01:30:00Z","bookingEligibility":{"allowed":true,"reasons":[]},
                         "serverTime":"2026-11-20T00:00:00Z"}
                        """, JsonCompareMode.STRICT));
    }
    @Test
    void absentSegmentAndMissingDescriptionRemainExplicitNulls() throws Exception {
        when(service.details(any())).thenReturn(new TripDetailsPolicy().evaluate(complete(), null, null, NOW));
        var response = mvc.perform(get(PATH).with(user("passenger"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingEligibility.allowed").value(true)).andReturn().getResponse();
        var body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.has("selectedSegment")).isTrue();
        org.assertj.core.api.Assertions.assertThat(body.get("selectedSegment").isNull()).isTrue();
        org.assertj.core.api.Assertions.assertThat(body.get("description").isNull()).isTrue();
    }
    @ParameterizedTest
    @ValueSource(strings = {"tripId", "unpaired", "duplicate", "blank"})
    void invalidInputsAre400BeforeService(String kind) throws Exception {
        var request = get(kind.equals("tripId") ? "/api/v1/trips/abc" : PATH).with(user("passenger"));
        if (kind.equals("unpaired")) request.param("fromStopId", A.toString());
        if (kind.equals("duplicate")) request.param("fromStopId", A.toString(), A.toString()).param("toStopId", C.toString());
        if (kind.equals("blank")) request.param("fromStopId", "").param("toStopId", C.toString());
        mvc.perform(request).andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }
    @Test
    void notFoundAndInvalidSegmentUseSafeContractErrors() throws Exception {
        when(service.details(any())).thenThrow(TripDetailsException.notFound());
        mvc.perform(get(PATH).with(user("passenger"))).andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
        doThrow(TripDetailsException.segment()).when(service).details(any());
        mvc.perform(get(PATH).with(user("passenger"))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_SEGMENT"));
    }
    @Test
    void databaseAndTransactionFailuresAre503WithoutPrivateCause() throws Exception {
        for (var failure : new RuntimeException[] {TripDetailsException.unavailable(new IllegalStateException("private SQL")),
                new CannotCreateTransactionException("private connection")}) {
            doThrow(failure).when(service).details(any());
            mvc.perform(get(PATH).with(user("passenger"))).andExpect(status().isServiceUnavailable())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().json("""
                            {"code":"TRIP_DATA_UNAVAILABLE","message":"Trip information could not be verified. Please retry.","fieldErrors":[]}
                            """, JsonCompareMode.STRICT));
        }
    }
    @Test
    void unexpectedFailureIsSafe500() throws Exception {
        when(service.details(any())).thenThrow(new IllegalStateException("private details"));
        mvc.perform(get(PATH).with(user("passenger"))).andExpect(status().isInternalServerError())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"code":"INTERNAL_ERROR","message":"The request could not be completed.","fieldErrors":[]}
                        """, JsonCompareMode.STRICT));
    }
    @Test
    void anonymousDetailsIsPublicAfterC2() throws Exception {
        when(service.details(any())).thenReturn(new TripDetailsPolicy().evaluate(complete(), null, null, NOW));
        mvc.perform(get(PATH)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tripId").value(DAY.toString()))
                .andExpect(jsonPath("$.bookingEligibility.allowed").value(true));
        verify(service).details(new vn.edu.team11.coach.trip.application.TripDetailsModels.Request(DAY, null, null));
    }
    @Test
    void partialResponseKeepsNullableSegmentPriceAndAllRequiredTopLevelKeys() throws Exception {
        var base = complete();
        var damaged = new TripDetailsPolicy.Facts(base.tripId(), base.status(), base.route(), true,
                base.vehicle(), true, base.capacity(), base.stops(), java.util.List.of(), base.seats());
        when(service.details(any())).thenReturn(new TripDetailsPolicy().evaluate(damaged, A, C, NOW));
        var response = mvc.perform(get(PATH).param("fromStopId", A.toString()).param("toStopId", C.toString())
                        .with(user("passenger"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingEligibility.allowed").value(false))
                .andExpect(jsonPath("$.bookingEligibility.reasons[0]").value("ESSENTIAL_DATA_INCOMPLETE"))
                .andReturn().getResponse();
        var body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.size()).isEqualTo(11);
        org.assertj.core.api.Assertions.assertThat(body.path("selectedSegment").size()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(body.path("selectedSegment").get("fareAmountVnd").isNull()).isTrue();
    }
}
