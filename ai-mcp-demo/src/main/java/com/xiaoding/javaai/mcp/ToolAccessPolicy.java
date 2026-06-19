package com.xiaoding.javaai.mcp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ToolAccessPolicy {

    private final Set<String> allowedTools;
    private final Map<String, String> requiredPermissions;

    private ToolAccessPolicy(Set<String> allowedTools, Map<String, String> requiredPermissions) {
        this.allowedTools = Set.copyOf(allowedTools);
        this.requiredPermissions = Map.copyOf(requiredPermissions);
    }

    public static ToolAccessPolicy allowlist(Set<String> toolNames) {
        Builder builder = builder();
        if (toolNames != null) {
            toolNames.forEach(builder::allowTool);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public ToolAccessDecision evaluate(String toolName, OperatorContext operator) {
        if (!allowedTools.contains(toolName)) {
            return ToolAccessDecision.deny("Tool " + toolName + " is not allowlisted for this host.");
        }
        String requiredPermission = requiredPermissions.get(toolName);
        if (requiredPermission != null && !operator.permissions().contains(requiredPermission)) {
            return ToolAccessDecision.deny("Tool " + toolName + " missing permission " + requiredPermission + ".");
        }
        return ToolAccessDecision.allow();
    }

    public static class Builder {
        private final Set<String> allowedTools = new HashSet<>();
        private final Map<String, String> requiredPermissions = new HashMap<>();

        public Builder allowTool(String toolName) {
            if (toolName != null && !toolName.isBlank()) {
                allowedTools.add(toolName);
            }
            return this;
        }

        public Builder requirePermission(String toolName, String permission) {
            if (toolName != null && !toolName.isBlank() && permission != null && !permission.isBlank()) {
                requiredPermissions.put(toolName, permission);
            }
            return this;
        }

        public ToolAccessPolicy build() {
            return new ToolAccessPolicy(allowedTools, requiredPermissions);
        }
    }
}
