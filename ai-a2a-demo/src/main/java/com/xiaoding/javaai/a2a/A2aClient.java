package com.xiaoding.javaai.a2a;

import java.util.List;

public class A2aClient {

    private final HelpdeskAgentSkillServer server;

    public A2aClient(HelpdeskAgentSkillServer server) {
        this.server = server;
    }

    public AgentTask createTask(AgentTaskRequest request) {
        return server.createTask(request);
    }

    public AgentTask getTask(String taskId) {
        return server.getTask(taskId);
    }

    public List<TaskEvent> streamTaskEvents(String taskId) {
        return server.events(taskId);
    }
}
