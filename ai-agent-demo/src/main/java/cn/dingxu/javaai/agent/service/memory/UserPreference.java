package cn.dingxu.javaai.agent.service.memory;

import java.time.Instant;

public record UserPreference(
        String tenantId,
        String userId,
        MemoryScope scope,
        String key,
        String value,
        Instant updatedAt
) {
    public UserPreference {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        value = value == null ? "" : value;
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }
}
