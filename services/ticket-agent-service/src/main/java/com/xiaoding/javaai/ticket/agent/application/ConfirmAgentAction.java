package com.xiaoding.javaai.ticket.agent.application;

@FunctionalInterface
public interface ConfirmAgentAction {
    ConfirmationDecisionReceipt decide(
            String taskId,
            ConfirmationActor actor,
            String idempotencyKey,
            ConfirmToolAction command
    );
}
