package com.xiaoding.javaai.knowledge.answer.application;

public record ModelStreamChunk(
        String delta,
        ModelStreamDecision decision,
        String model,
        ModelUsage usage,
        String finishReason
) {

    public ModelStreamChunk(
            String delta,
            String model,
            ModelUsage usage,
            String finishReason
    ) {
        this(delta, null, model, usage, finishReason);
    }
}
