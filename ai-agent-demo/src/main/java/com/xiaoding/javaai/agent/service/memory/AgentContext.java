package com.xiaoding.javaai.agent.service.memory;

import java.util.List;
import java.util.Optional;

public record AgentContext(
        String tenantId,
        String sessionId,
        String businessId,
        String userId,
        List<MemoryEntry> conversationWindow,
        Optional<BusinessSnapshot> businessSnapshot,
        List<UserPreference> preferences,
        List<String> sources
) {
    public AgentContext {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (businessId == null || businessId.isBlank()) {
            throw new IllegalArgumentException("businessId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        conversationWindow = conversationWindow == null ? List.of() : List.copyOf(conversationWindow);
        businessSnapshot = businessSnapshot == null ? Optional.empty() : businessSnapshot;
        preferences = preferences == null ? List.of() : List.copyOf(preferences);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
