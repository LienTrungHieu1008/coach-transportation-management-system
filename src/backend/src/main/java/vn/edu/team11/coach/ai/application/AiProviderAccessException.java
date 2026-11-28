package vn.edu.team11.coach.ai.application;

import java.util.Objects;

import vn.edu.team11.coach.ai.domain.FallbackReason;

public final class AiProviderAccessException extends RuntimeException {
    private static final int MAX_UPSTREAM_STATUS_LENGTH = 64;

    private final AiAccessErrorCode errorCode;
    private final FallbackReason fallbackReason;
    private final Integer upstreamHttpStatus;
    private final String upstreamStatus;

    public AiProviderAccessException(
            AiAccessErrorCode errorCode,
            String message,
            Integer upstreamHttpStatus,
            String upstreamStatus) {
        this(errorCode, message, upstreamHttpStatus, upstreamStatus, null);
    }

    public AiProviderAccessException(
            AiAccessErrorCode errorCode,
            String message,
            Integer upstreamHttpStatus,
            String upstreamStatus,
            Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.fallbackReason = errorCode.defaultFallbackReason();
        this.upstreamHttpStatus = upstreamHttpStatus;
        this.upstreamStatus = sanitizeStatus(upstreamStatus);
    }

    public AiAccessErrorCode errorCode() {
        return errorCode;
    }

    public FallbackReason fallbackReason() {
        return fallbackReason;
    }

    public Integer upstreamHttpStatus() {
        return upstreamHttpStatus;
    }

    public String upstreamStatus() {
        return upstreamStatus;
    }

    private static String sanitizeStatus(String rawStatus) {
        if (rawStatus == null) {
            return null;
        }
        String cleaned = rawStatus.replaceAll("[^A-Za-z0-9_.-]", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() <= MAX_UPSTREAM_STATUS_LENGTH
                ? cleaned
                : cleaned.substring(0, MAX_UPSTREAM_STATUS_LENGTH);
    }
}
