package cn.dingxu.javaai.a2a;

import java.util.List;

public record AgentCard(
        String name,
        String description,
        String endpoint,
        List<AgentSkill> skills
) {
    public AgentCard {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("agent name must not be blank");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("agent endpoint must not be blank");
        }
        description = description == null ? "" : description;
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
