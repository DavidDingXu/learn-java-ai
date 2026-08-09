package com.xiaoding.javaai.ticket.agent.infrastructure;

public final class AgentModelNotConfiguredException extends RuntimeException {
    public AgentModelNotConfiguredException() {
        super("ticket agent model is not configured");
    }
}
