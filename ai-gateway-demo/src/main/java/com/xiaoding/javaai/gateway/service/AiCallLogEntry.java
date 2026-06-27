package com.xiaoding.javaai.gateway.service;

import java.time.Instant;

public record AiCallLogEntry(
        String traceId,
        String userId,
        String scenario,
        String model,
        int attempt,
        String status,
        long latencyMs,
        String errorMessage,
        Instant createdAt
) {
    public static AiCallLogEntry success(String traceId,
                                         String userId,
                                         String scenario,
                                         String model,
                                         int attempt,
                                         long latencyMs) {
        return new AiCallLogEntry(traceId, userId, scenario, model, attempt, "SUCCESS", latencyMs, null, Instant.now());
    }

    public static AiCallLogEntry failed(String traceId,
                                        String userId,
                                        String scenario,
                                        String model,
                                        int attempt,
                                        long latencyMs,
                                        Throwable error) {
        return new AiCallLogEntry(traceId, userId, scenario, model, attempt, "FAILED", latencyMs, error.getMessage(), Instant.now());
    }
}
