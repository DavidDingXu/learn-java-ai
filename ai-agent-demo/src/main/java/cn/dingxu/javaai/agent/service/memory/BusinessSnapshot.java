package cn.dingxu.javaai.agent.service.memory;

import java.time.Instant;
import java.util.Map;

public record BusinessSnapshot(
        String tenantId,
        String sessionId,
        String businessId,
        Map<String, Object> fields,
        Instant capturedAt
) {
    public BusinessSnapshot {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (businessId == null || businessId.isBlank()) {
            throw new IllegalArgumentException("businessId must not be blank");
        }
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        capturedAt = capturedAt == null ? Instant.now() : capturedAt;
    }
}
