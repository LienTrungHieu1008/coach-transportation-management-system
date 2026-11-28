package vn.edu.team11.coach.ai.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class InputSanitizer {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?<![\\p{Alnum}_])(?:\\+84|84|0)(?:[\\s.-]?[1-9])(?:[\\s.-]?\\d){7,9}(?![\\p{Alnum}_-])");
    private static final List<String> UNSAFE_MARKERS = List.of(
            "<script",
            "</script",
            "javascript:",
            "ignore previous instructions",
            "ignore all previous instructions",
            "disregard previous instructions",
            "system prompt",
            "reveal your prompt",
            "bypass security");

    public SafeInputResult sanitizeOrReject(String rawText) {
        if (rawText == null) {
            return new SafeInputResult.Rejected(FallbackReason.UNSAFE_INPUT, "EMPTY_INPUT");
        }
        String normalized = Normalizer.normalize(rawText, Normalizer.Form.NFC);
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t') {
                return new SafeInputResult.Rejected(FallbackReason.UNSAFE_INPUT, "CONTROL_CHARACTER");
            }
        }

        String trimmed = normalized.trim();
        if (trimmed.isEmpty()) {
            return new SafeInputResult.Rejected(FallbackReason.UNSAFE_INPUT, "EMPTY_INPUT");
        }
        if (trimmed.length() > SafeAiInput.MAX_LENGTH) {
            return new SafeInputResult.Rejected(FallbackReason.UNSAFE_INPUT, "INPUT_TOO_LONG");
        }
        if (EMAIL_PATTERN.matcher(trimmed).find()) {
            return new SafeInputResult.Rejected(FallbackReason.UNSAFE_INPUT, "CONTAINS_EMAIL");
        }
        if (PHONE_PATTERN.matcher(trimmed).find()) {
            return new SafeInputResult.Rejected(FallbackReason.UNSAFE_INPUT, "CONTAINS_PHONE_NUMBER");
        }

        String lowered = trimmed.toLowerCase(Locale.ROOT);
        for (String marker : UNSAFE_MARKERS) {
            if (lowered.contains(marker)) {
                return new SafeInputResult.Rejected(FallbackReason.UNSAFE_INPUT, "UNSAFE_MARKUP_OR_INJECTION");
            }
        }

        return new SafeInputResult.Accepted(new SafeAiInput(trimmed));
    }
}
