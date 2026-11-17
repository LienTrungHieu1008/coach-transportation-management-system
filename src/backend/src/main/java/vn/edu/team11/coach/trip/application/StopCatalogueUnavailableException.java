package vn.edu.team11.coach.trip.application;

public class StopCatalogueUnavailableException extends RuntimeException {
    public StopCatalogueUnavailableException(Throwable cause) {
        super("Stop catalogue could not be retrieved", cause);
    }
}
