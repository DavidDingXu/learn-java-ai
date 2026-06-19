package cn.dingxu.javaai.a2a;

import java.util.Map;

public record AgentTaskRequest(String skillId, Map<String, Object> input) {
    public AgentTaskRequest {
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        input = input == null ? Map.of() : Map.copyOf(input);
    }
}
