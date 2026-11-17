package vn.edu.team11.coach.trip.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.team11.coach.identity.infrastructure.SecurityConfiguration;
import vn.edu.team11.coach.trip.application.TripSearchException;
import vn.edu.team11.coach.trip.application.TripSearchModels.*;
import vn.edu.team11.coach.trip.application.TripSearchService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

@WebMvcTest(TripSearchController.class)
@Import({SecurityConfiguration.class, TripSearchRequestParser.class})
class TripSearchControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private TripSearchService service;

    @Test
    void successUsesExactContractFieldsAndNoStore() throws Exception {
        when(service.search(any())).thenReturn(new Page(List.of(new Item(DAY,
                new Reference(ROUTE, "Route"), new Reference(A, "A"), new Reference(B, "B"),
                stops().getFirst().departure(), stops().get(1).arrival(), "Coach", 100_000, CUTOFF)),
                0, 20, 1, 1, NOW));
        mvc.perform(get("/api/v1/trips").params(TripSearchRequestParserTest.valid()).with(user("passenger")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"items":[{"tripId":"11111111-1111-4111-8111-111111111111",
                          "route":{"id":"44444444-4444-4444-8444-444444444444","name":"Route"},
                          "fromStop":{"id":"22222222-2222-4222-8222-222222222222","name":"A"},
                          "toStop":{"id":"33333333-3333-4333-8333-333333333333","name":"B"},
                          "boardingTime":"2026-11-20T02:00:00Z","alightingTime":"2026-11-20T04:00:00Z",
                          "vehicleType":"Coach","fareAmountVnd":100000,"bookingClosesAt":"2026-11-20T01:30:00Z"}],
                         "page":0,"size":20,"totalElements":1,"totalPages":1,"serverTime":"2026-11-20T00:00:00Z"}
                        """, JsonCompareMode.STRICT));
        verify(service).search(new Request(A, B, DATE, 0, 20));
    }

    @Test
    void emptyResultIs200Not404() throws Exception {
        when(service.search(any())).thenReturn(new Page(List.of(), 0, 20, 0, 0, NOW));
        mvc.perform(get("/api/v1/trips").params(TripSearchRequestParserTest.valid()).with(user("passenger")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void duplicateAndMissingParametersAre400BeforeCallingService() throws Exception {
        mvc.perform(get("/api/v1/trips").params(TripSearchRequestParserTest.valid())
                        .param("size", "20", "20").with(user("passenger")))
                .andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        mvc.perform(get("/api/v1/trips").with(user("passenger")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PAST_BOARDING_DATE", "STOP_NOT_SERVED", "INVALID_SEGMENT"})
    void businessErrorsRemain422(String code) throws Exception {
        when(service.search(any())).thenThrow(TripSearchException.criteria(code, "toStopId", "Invalid criteria."));
        mvc.perform(get("/api/v1/trips").params(TripSearchRequestParserTest.valid()).with(user("passenger")))
                .andExpect(status().isUnprocessableEntity()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void retrievalFailureIs503AndDoesNotExposeSql() throws Exception {
        when(service.search(any())).thenThrow(TripSearchException.unavailable(new IllegalStateException("private SQL")));
        mvc.perform(get("/api/v1/trips").params(TripSearchRequestParserTest.valid()).with(user("passenger")))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"code":"TRIP_DATA_UNAVAILABLE","message":"Trip information could not be verified. Please retry.","fieldErrors":[]}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void unexpectedFailureIs500AndDoesNotExposeStack() throws Exception {
        when(service.search(any())).thenThrow(new IllegalStateException("private details"));
        mvc.perform(get("/api/v1/trips").params(TripSearchRequestParserTest.valid()).with(user("passenger")))
                .andExpect(status().isInternalServerError()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"code":"INTERNAL_ERROR","message":"The request could not be completed.","fieldErrors":[]}
                        """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void anonymousWritesRemainBlockedBeforeReachingTripSearch(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/api/v1/trips"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(request(HttpMethod.valueOf(method), "/api/v1/trips").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(service);
    }
}
