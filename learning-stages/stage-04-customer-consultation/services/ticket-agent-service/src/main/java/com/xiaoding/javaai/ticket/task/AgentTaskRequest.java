package com.xiaoding.javaai.ticket.task;

import java.util.LinkedHashMap;
import java.util.Map;

public record AgentTaskRequest(
        String caseId,
        String objective,
        Map<String, String> businessContext
) {
    public AgentTaskRequest {
        caseId = requireText(caseId, "caseId", 64);
        objective = requireText(objective, "objective", 1000);
        businessContext = validateContext(businessContext);
    }

    private static Map<String, String> validateContext(Map<String, String> source) {
        if (source == null) throw new IllegalArgumentException("businessContext must not be null");
        if (source.size() > 32) throw new IllegalArgumentException("businessContext exceeds 32 entries");
        Map<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(
                requireText(key, "businessContext key", 128),
                requireText(value, "businessContext value", 2000)));
        return Map.copyOf(copy);
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " exceeds " + maxLength + " characters");
        }
        return normalized;
    }
}
