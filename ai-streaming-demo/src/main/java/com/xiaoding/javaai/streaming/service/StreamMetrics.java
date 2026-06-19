package com.xiaoding.javaai.streaming.service;

import java.time.Instant;

public record StreamMetrics(
        String sessionId,
        Instant requestStartedAt,
        Instant firstTokenAt,
        Instant completedAt,
        long ttftMs,
        long totalLatencyMs
) {
}
