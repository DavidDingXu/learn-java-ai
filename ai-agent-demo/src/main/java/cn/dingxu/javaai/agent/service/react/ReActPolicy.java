package cn.dingxu.javaai.agent.service.react;

import java.util.Set;

public record ReActPolicy(Set<String> allowedTools, int maxSteps, boolean readOnly) {

    public ReActPolicy {
        allowedTools = allowedTools == null ? Set.of() : Set.copyOf(allowedTools);
        if (maxSteps < 1) {
            throw new IllegalArgumentException("maxSteps must be greater than 0");
        }
    }

    public static ReActPolicy readOnly(Set<String> allowedTools, int maxSteps) {
        return new ReActPolicy(allowedTools, maxSteps, true);
    }

    public boolean allows(String toolName) {
        return allowedTools.contains(toolName);
    }
}
