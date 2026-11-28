package vn.edu.team11.coach.ai.domain;

import java.util.Objects;

public sealed interface SafeInputResult permits SafeInputResult.Accepted, SafeInputResult.Rejected {

    record Accepted(SafeAiInput input) implements SafeInputResult {
        public Accepted {
            Objects.requireNonNull(input, "input must not be null");
        }
    }

    record Rejected(FallbackReason fallbackReason, String rejectionCode) implements SafeInputResult {
        public Rejected {
            Objects.requireNonNull(fallbackReason, "fallbackReason must not be null");
            if (rejectionCode == null || rejectionCode.isBlank()) {
                throw new IllegalArgumentException("rejectionCode must not be blank");
            }
        }
    }
}
