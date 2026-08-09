package com.xiaoding.javaai.ticket.task;

public final class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String idempotencyKey) {
        super("idempotency key was reused with different business input: " + idempotencyKey);
    }
}
