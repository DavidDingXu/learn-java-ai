package com.xiaoding.javaai.agent.service.memory;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ConversationMemoryStore {

    private static final int DEFAULT_WINDOW_SIZE = 8;

    private final int windowSize;
    private final List<MemoryEntry> entries = new ArrayList<>();
    private final Map<String, BusinessSnapshot> snapshots = new LinkedHashMap<>();
    private final Map<String, UserPreference> preferences = new LinkedHashMap<>();

    public ConversationMemoryStore() {
        this(DEFAULT_WINDOW_SIZE);
    }

    public ConversationMemoryStore(int windowSize) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        this.windowSize = windowSize;
    }

    public void append(MemoryEntry entry) {
        entries.add(entry);
    }

    public List<MemoryEntry> recentConversation(String tenantId, String sessionId) {
        List<MemoryEntry> matched = entries.stream()
                .filter(entry -> entry.tenantId().equals(tenantId))
                .filter(entry -> entry.sessionId().equals(sessionId))
                .sorted(Comparator.comparing(MemoryEntry::occurredAt))
                .toList();
        int fromIndex = Math.max(0, matched.size() - windowSize);
        return List.copyOf(matched.subList(fromIndex, matched.size()));
    }

    public void saveBusinessSnapshot(BusinessSnapshot snapshot) {
        snapshots.put(snapshotKey(snapshot.tenantId(), snapshot.sessionId(), snapshot.businessId()), snapshot);
    }

    public Optional<BusinessSnapshot> businessSnapshot(String tenantId, String sessionId, String businessId) {
        return Optional.ofNullable(snapshots.get(snapshotKey(tenantId, sessionId, businessId)));
    }

    public void savePreference(UserPreference preference) {
        preferences.put(preferenceKey(preference.tenantId(), preference.userId(), preference.key()), preference);
    }

    public List<UserPreference> preferences(String tenantId, String userId) {
        return preferences.values().stream()
                .filter(preference -> preference.tenantId().equals(tenantId))
                .filter(preference -> preference.userId().equals(userId))
                .sorted(Comparator.comparing(UserPreference::updatedAt))
                .toList();
    }

    private String snapshotKey(String tenantId, String sessionId, String businessId) {
        return tenantId + ":" + sessionId + ":" + businessId;
    }

    private String preferenceKey(String tenantId, String userId, String key) {
        return tenantId + ":" + userId + ":" + key;
    }
}
