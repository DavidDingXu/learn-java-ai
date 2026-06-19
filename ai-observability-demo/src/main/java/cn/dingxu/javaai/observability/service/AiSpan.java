package cn.dingxu.javaai.observability.service;

import java.time.Instant;
import java.util.Map;

public record AiSpan(
        String spanId,
        String traceId,
        String name,
        SpanType type,
        Instant startedAt,
        Instant endedAt,
        Map<String, Object> attributes
) {
    public AiSpan {
        if (spanId == null || spanId.isBlank()) {
            throw new IllegalArgumentException("spanId must not be blank");
        }
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        startedAt = startedAt == null ? Instant.now() : startedAt;
        endedAt = endedAt == null ? startedAt : endedAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
