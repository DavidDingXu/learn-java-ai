package com.xiaoding.javaai.ticket.agent.application;

public final class ConfirmationIdempotencyConflictException extends RuntimeException {
    public ConfirmationIdempotencyConflictException(String key) {
        super("confirmation idempotency key was reused with a different decision: " + key);
    }
}
