package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.PreparedToolCall;
import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;
import com.xiaoding.javaai.ticket.task.AgentTask;

@FunctionalInterface
public interface AgentReadToolExecutor {
    ToolObservation execute(PreparedToolCall call, AgentTask task);
}
