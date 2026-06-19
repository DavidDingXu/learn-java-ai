package com.xiaoding.javaai.common.model;

public record AiChatResponse(
        String traceId,
        String model,
        String content,
        long latencyMs
) {
}
