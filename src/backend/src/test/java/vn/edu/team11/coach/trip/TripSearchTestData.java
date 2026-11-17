package vn.edu.team11.coach.trip;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.team11.coach.trip.contract.LayoutSeat;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.Candidate;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.LegFare;
import vn.edu.team11.coach.trip.domain.TripSearchPolicy.StopFact;

public final class TripSearchTestData {
    public static final UUID A = UUID.fromString("22222222-2222-4222-8222-222222222222");
    public static final UUID B = UUID.fromString("33333333-3333-4333-8333-333333333333");
    public static final UUID C = UUID.fromString("99999999-9999-4999-8999-999999999999");
    public static final UUID DAY = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID NIGHT = UUID.fromString("11111111-1111-4111-8111-111111111112");
    public static final UUID ROUTE = UUID.fromString("44444444-4444-4444-8444-444444444444");
    public static final LocalDate DATE = LocalDate.of(2026, 11, 20);
    public static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");
    public static final Instant CUTOFF = Instant.parse("2026-11-20T01:30:00Z");

    private TripSearchTestData() { }

    public static List<StopFact> stops() {
        return List.of(new StopFact(A, "A", 0, true, null, Instant.parse("2026-11-20T02:00:00Z")),
                new StopFact(B, "B", 1, true, Instant.parse("2026-11-20T04:00:00Z"), Instant.parse("2026-11-20T04:00:00Z")),
                new StopFact(C, "C", 2, true, Instant.parse("2026-11-20T06:00:00Z"), null));
    }

    public static Candidate trip(List<StopFact> stops, List<LegFare> fares, String status) {
        return new Candidate(DAY, ROUTE, "Route", status, UUID.fromString("66666666-6666-4666-8666-666666666666"),
                "Coach", true, UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), 1, stops, fares,
                List.of(new LayoutSeat(UUID.fromString("77777777-7777-4777-8777-777777777777"), "A1", 0, 0, 0)));
    }

    public static List<LegFare> fares() { return List.of(new LegFare(A, B, 100_000), new LegFare(B, C, 150_000)); }
    public static Candidate complete() { return trip(stops(), fares(), "Scheduled"); }
}
