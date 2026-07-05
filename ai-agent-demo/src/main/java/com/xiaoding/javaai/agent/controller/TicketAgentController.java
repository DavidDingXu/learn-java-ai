package com.xiaoding.javaai.agent.controller;

import com.xiaoding.javaai.common.ai.LiveAiResult;
import com.xiaoding.javaai.common.ai.SpringAiChatCaller;
import com.xiaoding.javaai.agent.service.TicketAgent;
import com.xiaoding.javaai.agent.service.TicketAgentRequest;
import com.xiaoding.javaai.agent.service.TicketAgentResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tickets")
public class TicketAgentController {

    private final TicketAgent ticketAgent;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public TicketAgentController(TicketAgent ticketAgent,
                                 ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                 @Value("${spring.ai.openai.api-key:}") String apiKey,
                                 @Value("${java-ai.agent.model-name:gpt-4o-mini}") String modelName) {
        this.ticketAgent = ticketAgent;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @PostMapping("/advice")
    public TicketAgentResult advice(@RequestBody TicketAgentRequest request) {
        return ticketAgent.handle(request);
    }

    @PostMapping("/live-advice")
    public LiveAgentAdvice liveAdvice(@RequestBody TicketAgentRequest request) {
        TicketAgentResult deterministicContext = ticketAgent.handle(request);
        LiveAiResult result = new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                modelName,
                "ai-agent-demo"
        ).call(
                "你是企业工单 Agent。根据后端已查询到的工具结果生成建议，不允许决定直接执行写操作。",
                """
                        用户问题：%s
                        后端 Agent 步骤：%s
                        确定性风险结论：requiresHumanApproval=%s
                        请输出：摘要、建议动作、为什么不能自动关闭工单、需要人工确认的条件。
                        """.formatted(request.userQuestion(), deterministicContext.steps(), deterministicContext.requiresHumanApproval())
        );
        return new LiveAgentAdvice(result.mode(), result.model(), result.content(), deterministicContext);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    public record LiveAgentAdvice(
            String mode,
            String model,
            String modelAdvice,
            TicketAgentResult backendContext
    ) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
