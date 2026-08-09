package com.xiaoding.javaai.ticket.agent.application;

public final class AgentCapacityExceededException extends RuntimeException {

    public AgentCapacityExceededException() {
        super("agent run capacity is full");
    }
}
