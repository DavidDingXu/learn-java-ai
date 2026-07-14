package com.xiaoding.javaai.labs.agentscope;

public record AgentExecutionIdentity(String tenantId, String subjectId, String role) {
    public AgentExecutionIdentity {
        if (tenantId == null || tenantId.isBlank() || subjectId == null || subjectId.isBlank()
                || role == null || role.isBlank()) {
            throw new IllegalArgumentException("identity fields must not be blank");
        }
    }
}
