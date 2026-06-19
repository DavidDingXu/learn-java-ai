package com.xiaoding.javaai.common.trace;

import java.time.Instant;
import java.util.UUID;

public record AiTrace(
        String traceId,
        String userId,
        String scenario,
        Instant startedAt
) {
    public static AiTrace start(String userId, String scenario) {
        return new AiTrace(UUID.randomUUID().toString(), userId, scenario, Instant.now());
    }
}
