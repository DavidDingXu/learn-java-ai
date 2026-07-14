package com.xiaoding.javaai.ticket.agent.infrastructure;

public final class AgentExternalIntegrationDisabledException extends RuntimeException {
    public AgentExternalIntegrationDisabledException(String integration) {
        super(integration + " is disabled in the current runtime profile");
    }
}
