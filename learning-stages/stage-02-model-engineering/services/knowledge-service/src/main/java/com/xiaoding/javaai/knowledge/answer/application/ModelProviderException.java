package com.xiaoding.javaai.knowledge.answer.application;

public final class ModelProviderException extends RuntimeException {

    private final Reason reason;

    public ModelProviderException(Reason reason, Throwable cause) {
        super("Chat model provider request failed", cause);
        if (reason == null) throw new IllegalArgumentException("reason must not be null");
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        RATE_LIMITED,
        UNAVAILABLE,
        REQUEST_REJECTED
    }
}
