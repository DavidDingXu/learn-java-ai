package com.xiaoding.javaai.agent.service;

import java.util.Map;

public record AgentStep(AgentStepType type, String name, Map<String, Object> output) {
    public AgentStep {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        output = output == null ? Map.of() : Map.copyOf(output);
    }
}
