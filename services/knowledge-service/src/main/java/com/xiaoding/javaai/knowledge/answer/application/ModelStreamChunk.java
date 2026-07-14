package com.xiaoding.javaai.knowledge.answer.application;

public record ModelStreamChunk(
        String delta,
        String model,
        ModelUsage usage,
        String finishReason
) {
}
