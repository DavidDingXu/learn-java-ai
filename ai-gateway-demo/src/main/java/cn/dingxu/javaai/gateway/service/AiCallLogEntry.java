package cn.dingxu.javaai.gateway.service;

import java.time.Instant;
import java.util.List;

public record AiCallLogEntry(
        String traceId,
        String userId,
        String scenario,
        String model,
        int attempt,
        String status,
        long latencyMs,
        String errorMessage,
        List<String> advisorEvents,
        Instant createdAt
) {
    public AiCallLogEntry {
        advisorEvents = advisorEvents == null ? List.of() : List.copyOf(advisorEvents);
    }

    public static AiCallLogEntry success(String traceId,
                                         String userId,
                                         String scenario,
                                         String model,
                                         int attempt,
                                         long latencyMs,
                                         List<String> advisorEvents) {
        return new AiCallLogEntry(traceId, userId, scenario, model, attempt, "SUCCESS", latencyMs, null, advisorEvents, Instant.now());
    }

    public static AiCallLogEntry failed(String traceId,
                                        String userId,
                                        String scenario,
                                        String model,
                                        int attempt,
                                        long latencyMs,
                                        Throwable error,
                                        List<String> advisorEvents) {
        return new AiCallLogEntry(traceId, userId, scenario, model, attempt, "FAILED", latencyMs, error.getMessage(), advisorEvents, Instant.now());
    }
}
