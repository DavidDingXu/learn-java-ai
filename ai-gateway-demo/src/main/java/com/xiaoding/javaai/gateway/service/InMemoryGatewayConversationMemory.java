package com.xiaoding.javaai.gateway.service;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGatewayConversationMemory implements GatewayConversationMemory {

    private final Map<String, List<String>> messages = new ConcurrentHashMap<>();

    @Override
    public void append(String userId, String message) {
        if (userId == null || userId.isBlank() || message == null || message.isBlank()) {
            return;
        }
        messages.computeIfAbsent(userId, ignored -> new ArrayList<>()).add(message);
    }

    @Override
    public List<String> recent(String userId, int limit) {
        List<String> all = messages.getOrDefault(userId, List.of());
        int safeLimit = Math.max(0, limit);
        int fromIndex = Math.max(0, all.size() - safeLimit);
        return List.copyOf(all.subList(fromIndex, all.size()));
    }
}
