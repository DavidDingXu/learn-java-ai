package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.task.AgentTask;

@FunctionalInterface
public interface RunAgentTask {
    AgentTask run(String taskId);
}
