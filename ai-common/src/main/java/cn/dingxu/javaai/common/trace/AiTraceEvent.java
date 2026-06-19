package cn.dingxu.javaai.common.trace;

import java.time.Instant;
import java.util.Map;

public record AiTraceEvent(
        String traceId,
        String name,
        Instant occurredAt,
        Map<String, Object> attributes
) {
    public static AiTraceEvent of(String traceId, String name, Map<String, Object> attributes) {
        return new AiTraceEvent(traceId, name, Instant.now(), Map.copyOf(attributes));
    }
}
