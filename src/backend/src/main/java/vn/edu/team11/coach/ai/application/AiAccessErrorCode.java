package vn.edu.team11.coach.ai.application;

import vn.edu.team11.coach.ai.domain.FallbackReason;

public enum AiAccessErrorCode {
    MISSING_API_KEY(FallbackReason.SERVICE_UNAVAILABLE),
    PROVIDER_AUTH_DENIED(FallbackReason.SERVICE_UNAVAILABLE),
    PROVIDER_QUOTA_EXCEEDED(FallbackReason.LIMIT_REACHED),
    PROVIDER_TIMEOUT(FallbackReason.TIMEOUT),
    PROVIDER_BAD_REQUEST(FallbackReason.SERVICE_UNAVAILABLE),
    PROVIDER_UNAVAILABLE(FallbackReason.SERVICE_UNAVAILABLE),
    PROVIDER_INVALID_RESPONSE(FallbackReason.SERVICE_UNAVAILABLE);

    private final FallbackReason defaultFallbackReason;

    AiAccessErrorCode(FallbackReason defaultFallbackReason) {
        this.defaultFallbackReason = defaultFallbackReason;
    }

    public FallbackReason defaultFallbackReason() {
        return defaultFallbackReason;
    }
}
