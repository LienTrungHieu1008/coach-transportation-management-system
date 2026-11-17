package vn.edu.team11.coach.trip.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.LinkedMultiValueMap;

import vn.edu.team11.coach.trip.application.TripSearchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.edu.team11.coach.trip.TripSearchTestData.*;

class TripSearchRequestParserTest {
    private final TripSearchRequestParser parser = new TripSearchRequestParser();

    static LinkedMultiValueMap<String, String> valid() {
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.set("fromStopId", A.toString());
        parameters.set("toStopId", B.toString());
        parameters.set("boardingDate", DATE.toString());
        return parameters;
    }

    @Test
    void defaultsAndMaximumPageAreParsed() {
        var request = parser.parse(valid());
        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        var parameters = valid();
        parameters.set("page", "2147483647");
        parameters.set("size", "100");
        assertThat(parser.parse(parameters).page()).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fromStopId", "toStopId", "boardingDate"})
    void requiredParameterMissingIs400(String field) {
        var parameters = valid();
        parameters.remove(field);
        assertInvalid(parameters, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fromStopId", "toStopId", "boardingDate", "page", "size"})
    void duplicateScalarEvenWithSameValueIs400(String field) {
        var parameters = valid();
        String value = parameters.getFirst(field);
        if (value == null) value = "1";
        parameters.put(field, List.of(value, value));
        assertInvalid(parameters, field);
    }

    @ParameterizedTest
    @CsvSource({"fromStopId,abc", "fromStopId,1-1-1-1-1", "toStopId,abc",
            "boardingDate,2026-02-30", "boardingDate,2026-1-01", "boardingDate,not-a-date",
            "page,-1", "page,2147483648", "page,1.5", "size,0", "size,101", "size,nope"})
    void malformedOrOutOfRangeInputIs400(String field, String value) {
        var parameters = valid();
        parameters.set(field, value);
        assertInvalid(parameters, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fromStopId", "toStopId", "boardingDate", "page", "size"})
    void emptyValuesDoNotUseDefaults(String field) {
        var parameters = valid();
        parameters.set(field, "");
        assertInvalid(parameters, field);
    }

    private void assertInvalid(LinkedMultiValueMap<String, String> parameters, String field) {
        assertThatThrownBy(() -> parser.parse(parameters)).isInstanceOfSatisfying(TripSearchException.class, exception -> {
            assertThat(exception.status()).isEqualTo(400);
            assertThat(exception.code()).isEqualTo("INVALID_REQUEST");
            assertThat(exception.fieldErrors().getFirst().field()).isEqualTo(field);
        });
    }
}
