package vn.edu.team11.coach.trip.infrastructure;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.team11.coach.trip.application.ServedStop;
import vn.edu.team11.coach.trip.application.StopCatalogueRepository;
import vn.edu.team11.coach.trip.application.StopCatalogueUnavailableException;
import vn.edu.team11.coach.trip.contract.TripStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/fixtures/trip_demo.sql")
class PostgresStopCatalogueRepositoryIT {
    private static final UUID A = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID B = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID C = UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final UUID ROUTE = UUID.fromString("44444444-4444-4444-8444-444444444444");

    @Autowired
    private StopCatalogueRepository repository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;

    @Test
    void activeStopsOnActiveRouteAreReturned() {
        assertThat(repository.findServedStops()).containsExactly(
                new ServedStop(A, "Demo Stop A"), new ServedStop(B, "Demo Stop B"),
                new ServedStop(C, "Demo Stop C"));
    }

    @Test
    void inactiveStopIsExcluded() {
        jdbc.update("UPDATE stop SET is_active = FALSE WHERE stop_id = ?", B);
        assertThat(ids()).containsExactly(A, C);
    }

    @Test
    void activeStopWithoutRouteIsExcluded() {
        UUID orphan = addStop("Orphan", true);
        assertThat(ids()).containsExactly(A, B, C).doesNotContain(orphan);
    }

    @Test
    void stopsOnOnlyInactiveRoutesProduceEmptyCatalogue() {
        jdbc.update("UPDATE route SET is_active = FALSE WHERE route_id = ?", ROUTE);
        assertThat(repository.findServedStops()).isEmpty();
    }

    @Test
    void multipleActiveAndInactiveRoutesDoNotDuplicateStops() {
        addRoute(A, true);
        addRoute(A, false);
        assertThat(ids()).containsExactly(A, B, C);
    }

    @Test
    void activeRouteStillServesStopWhenAnotherRouteIsInactive() {
        jdbc.update("UPDATE route SET is_active = FALSE WHERE route_id = ?", ROUTE);
        addRoute(A, true);
        assertThat(ids()).containsExactly(A);
    }

    @Test
    void routeWithoutAnyTripsStillServesItsStops() {
        UUID stop = addStop("No Trip Stop", true);
        UUID route = addRoute(stop, true);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip WHERE route_id = ?", Long.class, route))
                .isZero();
        assertThat(ids()).containsExactly(A, B, C, stop);
    }

    @ParameterizedTest
    @EnumSource(TripStatus.class)
    void tripStatusAndPastDepartureTimesDoNotAffectCatalogue(TripStatus status) {
        jdbc.update("UPDATE trip SET status = ?", status.name());
        jdbc.update("""
                UPDATE trip_stop SET planned_arrival_time = planned_arrival_time - INTERVAL '30 days',
                                     planned_departure_time = planned_departure_time - INTERVAL '30 days'
                """);
        assertThat(ids()).containsExactly(A, B, C);
    }

    @Test
    void sortsByNameThenPostgresUuidOrder() {
        UUID high = UUID.fromString("f0000000-0000-4000-8000-000000000001");
        UUID low = UUID.fromString("00000000-0000-4000-8000-000000000001");
        for (UUID id : List.of(high, low)) {
            jdbc.update("INSERT INTO stop(stop_id, stop_name, address) VALUES (?, 'Same name', 'Test')", id);
            addRoute(id, true);
        }
        UUID first = addStop("Alpha", true);
        UUID last = addStop("Zulu", true);
        addRoute(first, true);
        addRoute(last, true);
        assertThat(ids()).containsExactly(first, A, B, C, low, high, last);
    }

    @Test
    void catalogueIsNotLimitedToSearchDefaultPageSize() {
        for (int index = 0; index < 25; index++) {
            addRoute(addStop("Extra Stop " + index, true), true);
        }
        assertThat(repository.findServedStops()).hasSize(28);
    }

    @Test
    void databaseAccessFailureIsTypedRatherThanReturnedAsEmpty() {
        // Transaction-local search path causes a real PostgreSQL relation lookup failure;
        // rollback restores it without altering any shared schema or data.
        jdbc.execute("SET LOCAL search_path TO pg_catalog");
        assertThatThrownBy(repository::findServedStops)
                .isInstanceOf(StopCatalogueUnavailableException.class)
                .hasCauseInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void authenticatedHttpRequestReadsRealPostgresAndUsesContractEnvelope() throws Exception {
        jdbc.update("UPDATE stop SET is_active = FALSE WHERE stop_id = ?", B);
        addRoute(A, true);
        mvc.perform(get("/api/v1/stops").with(user("passenger")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(A.toString()))
                .andExpect(jsonPath("$.items[1].id").value(C.toString()))
                .andExpect(jsonPath("$.serverTime").isString());
        assertThat(jdbc.queryForObject("SELECT is_active FROM stop WHERE stop_id = ?", Boolean.class, B))
                .isFalse();
    }

    private List<UUID> ids() {
        return repository.findServedStops().stream().map(ServedStop::id).toList();
    }

    private UUID addStop(String name, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO stop(stop_id, stop_name, address, is_active) VALUES (?, ?, 'Test', ?)",
                id, name, active);
        return id;
    }

    private UUID addRoute(UUID stop, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO route(route_id, route_code, route_name, is_active) VALUES (?, ?, 'Test', ?)",
                id, id.toString(), active);
        jdbc.update("INSERT INTO route_stop(route_id, stop_id, stop_order) VALUES (?, ?, 0)", id, stop);
        return id;
    }
}
