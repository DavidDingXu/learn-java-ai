package cn.dingxu.javaai.observability.service;

import java.time.Instant;

public record AiTrace(String traceId, String userId, String scenario, Instant startedAt) {
    public AiTrace {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        startedAt = startedAt == null ? Instant.now() : startedAt;
    }
}
