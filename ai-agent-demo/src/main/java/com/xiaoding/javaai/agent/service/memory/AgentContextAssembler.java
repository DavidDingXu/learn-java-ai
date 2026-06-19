package com.xiaoding.javaai.agent.service.memory;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AgentContextAssembler {

    private final ConversationMemoryStore memoryStore;

    public AgentContextAssembler(ConversationMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    public AgentContext assemble(String tenantId, String sessionId, String businessId, String userId) {
        List<MemoryEntry> conversationWindow = memoryStore.recentConversation(tenantId, sessionId);
        Optional<BusinessSnapshot> businessSnapshot = memoryStore.businessSnapshot(tenantId, sessionId, businessId);
        List<UserPreference> preferences = memoryStore.preferences(tenantId, userId);
        List<String> sources = new ArrayList<>();

        sources.add("conversation");
        businessSnapshot.ifPresent(snapshot -> sources.add("businessSnapshot"));
        if (!preferences.isEmpty()) {
            sources.add("preferences");
        }

        return new AgentContext(
                tenantId,
                sessionId,
                businessId,
                userId,
                conversationWindow,
                businessSnapshot,
                preferences,
                sources
        );
    }
}
