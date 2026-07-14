package com.xiaoding.javaai.ticket.task;

public final class AgentTaskAccessDeniedException extends RuntimeException {
    public AgentTaskAccessDeniedException(String reason) {
        super(reason == null || reason.isBlank() ? "agent task access is denied" : reason.trim());
    }
}
