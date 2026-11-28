package vn.edu.team11.coach.ai.application;

public record LlmGenerationResult(
        String text,
        String model,
        String finishReason,
        long latencyMs) {
}
