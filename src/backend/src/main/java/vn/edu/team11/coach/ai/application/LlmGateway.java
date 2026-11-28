package vn.edu.team11.coach.ai.application;

import java.time.Instant;

import vn.edu.team11.coach.ai.domain.SafeAiInput;

public interface LlmGateway {
    LlmGenerationResult generateText(String systemInstruction, SafeAiInput userInput, Instant deadline);
}
