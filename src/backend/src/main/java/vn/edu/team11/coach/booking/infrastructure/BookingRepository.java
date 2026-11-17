package vn.edu.team11.coach.booking.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.team11.coach.booking.domain.BookingStatus;
import vn.edu.team11.coach.booking.domain.SeatAllocation;
import vn.edu.team11.coach.booking.domain.Segment;

@Repository
public class BookingRepository {
    private final JdbcTemplate jdbc;

    public BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int expireDueBookings(Instant asOf) {
        OffsetDateTime timestamp = Objects.requireNonNull(asOf, "asOf").atOffset(ZoneOffset.UTC);
        return jdbc.queryForObject("""
                with due as (
                    select booking_id from booking
                    where status = 'Pending' and expires_at <= ?
                    order by booking_id for update skip locked
                ), expired as (
                    update booking b set status = 'Expired' from due
                    where b.booking_id = due.booking_id and b.status = 'Pending' and b.expires_at <= ?
                    returning b.booking_id
                ), released as (
                    update seat_allocation a set released_at = ? from expired
                    where a.booking_id = expired.booking_id and a.kind = 'Hold' and a.released_at is null
                    returning a.seat_allocation_id
                )
                select count(*) from expired
                """, Integer.class, timestamp, timestamp, timestamp);
    }

    public List<SeatAllocation> findCandidates(UUID tripId, Segment segment) {
        Objects.requireNonNull(tripId, "tripId");
        Objects.requireNonNull(segment, "segment");
        return jdbc.query("""
                select a.trip_id, a.physical_seat_id, a.from_stop_index, a.to_stop_index,
                    b.status, a.kind, a.expires_at, a.released_at
                from seat_allocation a join booking b on b.booking_id = a.booking_id
                where a.trip_id = ? and a.from_stop_index < ? and a.to_stop_index > ?
                    and a.released_at is null
                """, (row, rowNumber) -> {
                    OffsetDateTime expiresAt = row.getObject("expires_at", OffsetDateTime.class);
                    OffsetDateTime releasedAt = row.getObject("released_at", OffsetDateTime.class);
                    return new SeatAllocation(row.getObject("trip_id", UUID.class),
                            row.getObject("physical_seat_id", UUID.class),
                            new Segment(row.getInt("from_stop_index"), row.getInt("to_stop_index")),
                            BookingStatus.valueOf(row.getString("status")),
                            SeatAllocation.Kind.valueOf(row.getString("kind")),
                            expiresAt == null ? null : expiresAt.toInstant(),
                            releasedAt == null ? null : releasedAt.toInstant());
                }, tripId, segment.toStopIndex(), segment.fromStopIndex());
    }
}
