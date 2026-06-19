package com.xiaoding.javaai.observability.service;

import java.math.BigDecimal;
import java.time.Instant;

public record ModelUsage(
        String traceId,
        String model,
        int inputTokens,
        int outputTokens,
        BigDecimal cost,
        Instant recordedAt
) {
    public ModelUsage {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("tokens must not be negative");
        }
        cost = cost == null ? BigDecimal.ZERO : cost;
        recordedAt = recordedAt == null ? Instant.now() : recordedAt;
    }
}
