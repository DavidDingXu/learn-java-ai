package com.xiaoding.javaai.legacy.ticket;

public final class ConfirmationOutcomeUnknownException extends RuntimeException {

    private final String idempotencyKey;

    public ConfirmationOutcomeUnknownException(String idempotencyKey, Throwable cause) {
        super("confirmation outcome is unknown; reuse the same idempotency key", cause);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
