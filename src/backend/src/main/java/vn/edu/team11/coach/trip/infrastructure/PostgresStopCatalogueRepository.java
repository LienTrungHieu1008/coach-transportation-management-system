package vn.edu.team11.coach.trip.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.team11.coach.trip.application.ServedStop;
import vn.edu.team11.coach.trip.application.StopCatalogueRepository;
import vn.edu.team11.coach.trip.application.StopCatalogueUnavailableException;

@Repository
public class PostgresStopCatalogueRepository implements StopCatalogueRepository {
    private final JdbcTemplate jdbc;

    public PostgresStopCatalogueRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ServedStop> findServedStops() {
        try {
            return jdbc.query("""
                    SELECT s.stop_id, s.stop_name
                    FROM stop s
                    WHERE s.is_active = TRUE
                      AND EXISTS (
                          SELECT 1
                          FROM route_stop rs
                          JOIN route r ON r.route_id = rs.route_id
                          WHERE rs.stop_id = s.stop_id
                            AND r.is_active = TRUE
                      )
                    ORDER BY s.stop_name ASC, s.stop_id ASC
                    """, (rs, rowNum) -> new ServedStop(
                    rs.getObject("stop_id", UUID.class), rs.getString("stop_name")));
        } catch (DataAccessException exception) {
            throw new StopCatalogueUnavailableException(exception);
        }
    }
}
