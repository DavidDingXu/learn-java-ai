package com.xiaoding.javaai.ticket.agent.application;

public final class ReadToolUnavailableException extends RuntimeException {

    public enum FailureKind {
        REQUEST_REJECTED,
        DEPENDENCY_UNAVAILABLE,
        TRANSPORT_FAILURE,
        INVALID_RESPONSE
    }

    private final FailureKind failureKind;

    public ReadToolUnavailableException(FailureKind failureKind, Throwable cause) {
        super("read tool dependency failed: "
                + java.util.Objects.requireNonNull(failureKind, "failureKind must not be null").name(), cause);
        this.failureKind = failureKind;
    }

    public FailureKind failureKind() {
        return failureKind;
    }
}
