package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.a2a.agent.A2aAgentConfig;

public final class AgentScopeA2aClientBoundary {

    private AgentScopeA2aClientBoundary() {
    }

    public static A2aAgentConfig defaultConfig() {
        return A2aAgentConfig.builder().build();
    }
}
