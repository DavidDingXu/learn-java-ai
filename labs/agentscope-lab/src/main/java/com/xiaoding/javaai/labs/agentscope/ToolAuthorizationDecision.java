package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.permission.PermissionBehavior;

public record ToolAuthorizationDecision(
        String tenantId,
        String subjectId,
        String toolName,
        PermissionBehavior behavior,
        String reason) {
}
