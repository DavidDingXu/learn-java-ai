package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;

public record ConfirmationDecisionReceipt(
        String taskId,
        AgentTaskState state,
        String actionId,
        String toolStatus,
        String auditId,
        long taskVersion,
        boolean duplicate
) {
    public ConfirmationDecisionReceipt asDuplicate() {
        return new ConfirmationDecisionReceipt(
                taskId, state, actionId, toolStatus, auditId, taskVersion, true);
    }
}
