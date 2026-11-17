package vn.edu.team11.coach.trip.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import vn.edu.team11.coach.trip.application.ServedStop;
import vn.edu.team11.coach.trip.application.StopCatalogueService;
import vn.edu.team11.coach.trip.application.StopCatalogueUnavailableException;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StopController.class)
@Import(SecurityConfiguration.class)
class StopControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private StopCatalogueService service;

    @Test
    void returnsOnlyContractFieldsWithUtcTimeAndNoStore() throws Exception {
        var stop = new ServedStop(UUID.fromString("22222222-2222-4222-8222-222222222222"), "Stop A");
        when(service.listStops()).thenReturn(new StopCatalogueService.Catalogue(
                List.of(stop), Instant.parse("2026-11-17T08:00:00Z")));
        mvc.perform(get("/api/v1/stops").with(user("passenger")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("""
                        {"items":[{"id":"22222222-2222-4222-8222-222222222222","name":"Stop A"}],
                         "serverTime":"2026-11-17T08:00:00Z"}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void emptyCatalogueStillReturns200AndEmptyArray() throws Exception {
        when(service.listStops()).thenReturn(new StopCatalogueService.Catalogue(
                List.of(), Instant.parse("2026-11-17T08:00:00Z")));
        mvc.perform(get("/api/v1/stops").with(user("passenger")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"items":[],"serverTime":"2026-11-17T08:00:00Z"}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void databaseFailureReturns503WithoutLeakingCause() throws Exception {
        when(service.listStops()).thenThrow(new StopCatalogueUnavailableException(
                new IllegalStateException("SELECT secret_database_details")));
        mvc.perform(get("/api/v1/stops").with(user("passenger")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"code":"TRIP_DATA_UNAVAILABLE",
                         "message":"Trip information could not be verified. Please retry.","fieldErrors":[]}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingCause() throws Exception {
        when(service.listStops()).thenThrow(new IllegalStateException("private internal details"));
        mvc.perform(get("/api/v1/stops").with(user("passenger")))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"code":"INTERNAL_ERROR","message":"The request could not be completed.","fieldErrors":[]}
                        """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void anonymousWritesRemainBlockedBeforeReachingStopCatalogue(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/api/v1/stops"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(request(HttpMethod.valueOf(method), "/api/v1/stops").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(service);
    }
}
