package com.xiaoding.javaai.ticket.agent.web;

import com.xiaoding.javaai.ticket.agent.application.ConfirmationDecisionReceipt;

public record ConfirmationDecisionReceiptResponse(
        String taskId,
        String state,
        String actionId,
        String toolStatus,
        String auditId,
        long taskVersion,
        boolean duplicate
) {
    static ConfirmationDecisionReceiptResponse from(ConfirmationDecisionReceipt receipt) {
        return new ConfirmationDecisionReceiptResponse(
                receipt.taskId(), receipt.state().name(), receipt.actionId(),
                receipt.toolStatus(), receipt.auditId(), receipt.taskVersion(), receipt.duplicate());
    }
}
