package com.xiaoding.javaai.ticket.agent.application;

public final class AgentRunUnavailableException extends RuntimeException {

    private final String reasonCode;

    public AgentRunUnavailableException(String reasonCode, Throwable cause) {
        super("agent run dependency is unavailable", cause);
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("reasonCode must not be blank");
        }
        this.reasonCode = reasonCode.trim();
    }

    public String reasonCode() {
        return reasonCode;
    }
}
