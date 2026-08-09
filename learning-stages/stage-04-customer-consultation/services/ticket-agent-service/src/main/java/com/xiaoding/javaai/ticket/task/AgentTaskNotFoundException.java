package com.xiaoding.javaai.ticket.task;

public final class AgentTaskNotFoundException extends RuntimeException {
    public AgentTaskNotFoundException(String taskId) {
        super("agent task was not found: " + taskId);
    }
}
