package com.xiaoding.javaai.ticket.agent.application;

public record AgentModelUsage(int promptTokens, int completionTokens, int totalTokens) {
    public AgentModelUsage {
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("agent model token usage must not be negative");
        }
    }

    public static AgentModelUsage unknown() {
        return new AgentModelUsage(0, 0, 0);
    }
}
