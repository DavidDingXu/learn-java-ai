package cn.dingxu.javaai.a2a;

import java.util.Map;

public record AgentSkill(
        String id,
        String name,
        String description,
        Map<String, String> inputSchema
) {
    public AgentSkill {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("skill id must not be blank");
        }
        name = name == null ? id : name;
        description = description == null ? "" : description;
        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
    }
}
