package com.xiaoding.javaai.ticket.agent.infrastructure;

public final class ConfirmationDecisionInProgressException extends IllegalStateException {

    public ConfirmationDecisionInProgressException(String idempotencyKey) {
        super("confirmation decision is already in progress for idempotency key: " + idempotencyKey);
    }
}
