package com.xiaoding.javaai.a2a;

import com.xiaoding.javaai.common.ai.LiveAiResult;
import com.xiaoding.javaai.common.ai.SpringAiChatCaller;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/a2a")
public class A2aDemoController {

    private final HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();
    private final A2aClient client = new A2aClient(server);
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public A2aDemoController(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                             @Value("${spring.ai.openai.api-key:}") String apiKey,
                             @Value("${java-ai.a2a.model-name:gpt-4o-mini}") String modelName) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

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

    @PostMapping("/tasks/live")
    public LiveA2aTaskResult createLiveTask(@RequestBody AgentTaskRequest request) {
        AgentTask task = client.createTask(request);
        List<TaskEvent> events = client.streamTaskEvents(task.taskId());
        LiveAiResult answer = new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                modelName,
                "ai-a2a-demo"
        ).call(
                "你是企业工单 AI 助手。必须基于 A2A task artifacts 和事件状态回答，不能声称已经执行写操作。",
                """
                        Agent Card：%s
                        Task：%s
                        Events：%s
                        请给调用方输出一段可展示的处理建议，并说明是否需要继续提交人工输入。
                        """.formatted(server.agentCard(), task, events)
        );
        return new LiveA2aTaskResult(task, events, answer);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    public record LiveA2aTaskResult(
            AgentTask task,
            List<TaskEvent> events,
            LiveAiResult answer
    ) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
