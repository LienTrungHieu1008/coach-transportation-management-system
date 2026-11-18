package vn.edu.team11.coach.trip.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.LinkedMultiValueMap;
import vn.edu.team11.coach.trip.application.TripDetailsException;
import static org.assertj.core.api.Assertions.*;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

class TripDetailsRequestParserTest {
    private final TripDetailsRequestParser parser = new TripDetailsRequestParser();
    @Test
    void acceptsAbsentPairAndCanonicalUuidWithUppercase() {
        var parameters = new LinkedMultiValueMap<String, String>();
        assertThat(parser.parse(DAY.toString(), parameters).fromStopId()).isNull();
        parameters.add("fromStopId", A.toString().toUpperCase());
        parameters.add("toStopId", C.toString());
        assertThat(parser.parse(DAY.toString(), parameters).fromStopId()).isEqualTo(A);
    }
    @ParameterizedTest
    @ValueSource(strings = {"fromStopId", "toStopId"})
    void rejectsUnpairedAndDuplicateScalar(String field) {
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.add(field, A.toString());
        invalid(() -> parser.parse(DAY.toString(), parameters));
        parameters.set("fromStopId", A.toString());
        parameters.set("toStopId", C.toString());
        parameters.add(field, parameters.getFirst(field));
        invalid(() -> parser.parse(DAY.toString(), parameters));
    }
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "1-1-1-1-1", "22222222222242228222222222222222"})
    void rejectsMalformedPathOrQueryUuid(String value) {
        invalid(() -> parser.parse(value, new LinkedMultiValueMap<>()));
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("fromStopId", value);
        parameters.add("toStopId", C.toString());
        invalid(() -> parser.parse(DAY.toString(), parameters));
        parameters.set("fromStopId", A.toString());
        parameters.set("toStopId", value);
        invalid(() -> parser.parse(DAY.toString(), parameters));
    }
    private void invalid(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(TripDetailsException.class,
                e -> assertThat(e.status()).isEqualTo(400));
    }
}
