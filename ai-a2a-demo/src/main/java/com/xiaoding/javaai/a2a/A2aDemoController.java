package com.xiaoding.javaai.a2a;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/a2a")
public class A2aDemoController {

    private final HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();
    private final A2aClient client = new A2aClient(server);

    @GetMapping("/card")
    public AgentCard card() {
        return server.agentCard();
    }

    @PostMapping("/tasks")
    public AgentTask createTask(@RequestBody AgentTaskRequest request) {
        return client.createTask(request);
    }

    @GetMapping("/tasks/{taskId}")
    public AgentTask getTask(@PathVariable("taskId") String taskId) {
        return client.getTask(taskId);
    }

    @GetMapping("/tasks/{taskId}/events")
    public List<TaskEvent> events(@PathVariable("taskId") String taskId) {
        return client.streamTaskEvents(taskId);
    }

    @PostMapping("/tasks/{taskId}/input")
    public AgentTask submitInput(@PathVariable("taskId") String taskId, @RequestBody Map<String, Object> input) {
        return server.submitInput(taskId, input == null ? Map.of() : input);
    }
}
