package com.xiaoding.javaai.helpdesk.agent;

import com.xiaoding.javaai.common.ai.LiveAiResult;
import com.xiaoding.javaai.common.ai.SpringAiChatCaller;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/helpdesk-agent")
public class HelpdeskAgentController {

    private final HelpdeskAgentApplicationService agentService;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public HelpdeskAgentController(HelpdeskAgentApplicationService agentService,
                                   ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                   @Value("${spring.ai.openai.api-key:}") String apiKey,
                                   @Value("${java-ai.helpdesk-agent.model-name:gpt-4o-mini}") String modelName) {
        this.agentService = agentService;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @GetMapping("/scenarios/refund")
    public HelpdeskAgentScenarioReport refundScenario() {
        return agentService.runRefundScenario();
    }

    @PostMapping("/advice")
    public AgentAdviceResult advice(@RequestBody AdviceHttpRequest request) {
        return agentService.advise(new AgentAdviceRequest(
                request.ticketId(),
                request.question(),
                request.operator()
        ));
    }

    @PostMapping("/advice/live")
    public LiveAgentAdviceResult liveAdvice(@RequestBody AdviceHttpRequest request) {
        AgentAdviceResult context = agentService.advise(new AgentAdviceRequest(
                request.ticketId(),
                request.question(),
                request.operator()
        ));
        LiveAiResult answer = new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                modelName,
                "project-helpdesk-agent"
        ).call(
                "你是企业工单 AI 助手。必须基于 Tool、RAG 引用、风险等级和人工确认要求生成建议；不能直接执行退款、关闭工单或承诺已经写入业务系统。",
                """
                        工单请求：%s
                        Java Agent 已完成的上下文编排结果：%s
                        Tool 调用记录：%s
                        Trace：%s
                        请输出客服可读的处理建议，明确下一步动作和人工确认边界。
                        """.formatted(
                        request,
                        context,
                        context.toolRecords(),
                        context.trace()
                )
        );
        return new LiveAgentAdviceResult(context, answer);
    }

    @PostMapping("/tickets/close")
    public ToolResult closeTicket(@RequestBody CloseTicketHttpRequest request) {
        return agentService.closeTicket(
                request.ticketId(),
                request.humanApproved(),
                request.confirmationToken(),
                request.operator()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    public record AdviceHttpRequest(
            String ticketId,
            String question,
            String userId,
            String tenantId,
            String department
    ) {
        OperatorContext operator() {
            return new OperatorContext(userId, tenantId, department);
        }
    }

    public record CloseTicketHttpRequest(
            String ticketId,
            boolean humanApproved,
            String confirmationToken,
            String userId,
            String tenantId,
            String department
    ) {
        OperatorContext operator() {
            return new OperatorContext(userId, tenantId, department);
        }
    }

    public record LiveAgentAdviceResult(
            AgentAdviceResult context,
            LiveAiResult answer
    ) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
