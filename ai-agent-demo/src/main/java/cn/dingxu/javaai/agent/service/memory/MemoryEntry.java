package cn.dingxu.javaai.agent.service.memory;

import java.time.Instant;
import java.util.Map;

public record MemoryEntry(
        String tenantId,
        String sessionId,
        String userId,
        MemoryEntryType type,
        String content,
        Map<String, Object> metadata,
        Instant occurredAt
) {
    public MemoryEntry {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        content = content == null ? "" : content;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
