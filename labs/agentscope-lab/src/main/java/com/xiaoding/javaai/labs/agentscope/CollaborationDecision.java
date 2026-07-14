package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.event.RequireUserConfirmEvent;

public record CollaborationDecision(
        CollaborationMode mode,
        String reason,
        RequireUserConfirmEvent confirmationEvent) {
}
