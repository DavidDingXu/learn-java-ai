package com.xiaoding.javaai.ticket.agent.domain;

public enum AgentTaskState {
    ACCEPTED,
    RUNNING,
    WAITING_CONFIRMATION,
    EXECUTING,
    EXECUTION_UNCERTAIN,
    COMPLETED,
    REJECTED,
    FAILED
}
