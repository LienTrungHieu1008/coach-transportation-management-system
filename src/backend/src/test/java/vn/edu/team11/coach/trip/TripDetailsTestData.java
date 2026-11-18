package vn.edu.team11.coach.trip;

import java.util.List;
import vn.edu.team11.coach.trip.contract.TripStatus;
import vn.edu.team11.coach.trip.domain.TripDetailsPolicy.*;

public final class TripDetailsTestData {
    private TripDetailsTestData() { }
    public static Facts complete() {
        var p2 = TripSearchTestData.complete();
        return new Facts(p2.id(), TripStatus.Scheduled, new Reference(p2.routeId(), p2.routeName()), true,
                new Vehicle(p2.vehicleId(), p2.vehicleType(), p2.layoutId()), true, p2.capacity(),
                p2.stops().stream().map(stop -> new StopFact(stop.id(), stop.name(), stop.index(), true,
                        stop.configured(), stop.arrival(), stop.departure())).toList(),
                p2.fares().stream().map(fare -> new Fare(fare.from(), fare.to(), fare.amount())).toList(), p2.seats());
    }
    public static Facts facts(TripStatus status, List<StopFact> stops, List<Fare> fares) {
        var base = complete();
        return new Facts(base.tripId(), status, base.route(), true, base.vehicle(), true,
                base.capacity(), stops, fares, base.seats());
    }
}
