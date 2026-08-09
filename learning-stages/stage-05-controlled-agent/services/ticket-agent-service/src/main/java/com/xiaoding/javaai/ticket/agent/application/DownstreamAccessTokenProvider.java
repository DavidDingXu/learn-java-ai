package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.task.AgentTask;

@FunctionalInterface
public interface DownstreamAccessTokenProvider {
    String tokenFor(AgentTask task, String audience, String scope);
}
