package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.DownstreamAccessTokenProvider;
import com.xiaoding.javaai.ticket.task.AgentTask;

final class LocalDownstreamAccessTokenProvider implements DownstreamAccessTokenProvider {

    @Override
    public String tokenFor(AgentTask task, String audience, String scope) {
        java.util.Objects.requireNonNull(task, "task must not be null");
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("audience must not be blank");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }
        return "local-delegated-access";
    }
}
