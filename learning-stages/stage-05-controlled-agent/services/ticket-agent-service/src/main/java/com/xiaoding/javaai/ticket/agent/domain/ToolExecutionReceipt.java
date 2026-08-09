package com.xiaoding.javaai.ticket.agent.domain;

public record ToolExecutionReceipt(
        String actionId,
        String status,
        boolean duplicate,
        String auditId
) {
    public ToolExecutionReceipt {
        actionId = requireText(actionId, "actionId");
        status = requireText(status, "status");
        if (!"SUCCEEDED".equals(status)) {
            throw new IllegalArgumentException("status must be SUCCEEDED");
        }
        auditId = requireText(auditId, "auditId");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
