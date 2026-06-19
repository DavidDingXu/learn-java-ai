package cn.dingxu.javaai.observability.service;

import java.time.Instant;
import java.util.Map;

public record AiEvent(
        String eventId,
        String traceId,
        String name,
        Instant occurredAt,
        Map<String, Object> attributes
) {
    public AiEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
