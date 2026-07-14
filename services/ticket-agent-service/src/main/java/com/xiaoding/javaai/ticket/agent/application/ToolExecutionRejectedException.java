package com.xiaoding.javaai.ticket.agent.application;

public final class ToolExecutionRejectedException extends RuntimeException {

    private final String code;

    public ToolExecutionRejectedException(String code, String message) {
        super(requireText(code, "code") + ": " + requireText(message, "message"));
        this.code = requireText(code, "code");
    }

    public String code() {
        return code;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
