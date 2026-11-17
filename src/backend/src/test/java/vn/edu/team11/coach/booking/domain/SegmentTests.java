package vn.edu.team11.coach.booking.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SegmentTests {
    @ParameterizedTest
    @CsvSource({"-1, 1", "0, 0", "2, 1", "0, -1"})
    void rejectsInvalidStopIndices(int from, int to) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Segment(from, to));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1, 1, 2, false",
            "0, 1, 2, 3, false",
            "0, 1, 0, 1, true",
            "0, 2, 1, 3, true",
            "0, 3, 1, 2, true",
            "0, 3, 0, 2, true",
            "0, 3, 1, 3, true",
            "0, 2147483647, 2147483646, 2147483647, true"
    })
    void overlapUsesHalfOpenIntervalsInBothDirections(int from, int to, int otherFrom, int otherTo,
                                                     boolean expected) {
        var segment = new Segment(from, to);
        var other = new Segment(otherFrom, otherTo);

        assertThat(segment.overlaps(other)).isEqualTo(expected);
        assertThat(other.overlaps(segment)).isEqualTo(expected);
    }

    @Test
    void rejectsMissingSegmentInsteadOfAssumingNoOverlap() {
        assertThatNullPointerException().isThrownBy(() -> new Segment(0, 1).overlaps(null));
    }
}
