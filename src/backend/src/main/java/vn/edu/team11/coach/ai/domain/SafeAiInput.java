package vn.edu.team11.coach.ai.domain;

public record SafeAiInput(String text) {
    public static final int MAX_LENGTH = 500;

    public SafeAiInput {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("SafeAiInput text must not be blank");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("SafeAiInput text exceeds max length " + MAX_LENGTH);
        }
    }
}
