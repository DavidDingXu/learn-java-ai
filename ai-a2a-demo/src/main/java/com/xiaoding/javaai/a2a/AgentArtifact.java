package com.xiaoding.javaai.a2a;

import java.util.Map;

public record AgentArtifact(String name, String mimeType, Map<String, Object> content) {
    public AgentArtifact {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("artifact name must not be blank");
        }
        mimeType = mimeType == null ? "application/json" : mimeType;
        content = content == null ? Map.of() : Map.copyOf(content);
    }
}
