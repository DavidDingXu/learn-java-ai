package com.xiaoding.javaai.tool.controller;

import com.xiaoding.javaai.common.ai.RealAiRuntime;
import com.xiaoding.javaai.tool.service.OperatorContext;
import com.xiaoding.javaai.tool.service.TicketToolFacade;
import com.xiaoding.javaai.tool.service.ToolExecutionLedger;
import com.xiaoding.javaai.tool.service.ToolExecutionRecord;
import com.xiaoding.javaai.tool.service.ToolResult;
import com.xiaoding.javaai.tool.service.springai.SpringAiToolCallbackBridge;
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

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final TicketToolFacade ticketToolFacade;
    private final ToolExecutionLedger ledger;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public ToolController(TicketToolFacade ticketToolFacade,
                          ToolExecutionLedger ledger,
                          ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                          @Value("${spring.ai.openai.api-key:}") String apiKey,
                          @Value("${java-ai.tool.model-name:gpt-4o-mini}") String modelName) {
        this.ticketToolFacade = ticketToolFacade;
        this.ledger = ledger;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @PostMapping("/ticket/lookup")
    public ToolResult lookupTicket(@RequestBody TicketLookupRequest request) {
        return ticketToolFacade.lookupTicket(request.ticketId(), request.operator());
    }

    @PostMapping("/ticket/close")
    public ToolResult closeTicket(@RequestBody TicketCloseRequest request) {
        return ticketToolFacade.closeTicket(
                request.ticketId(),
                request.humanApproved(),
                request.confirmationToken(),
                request.operator()
        );
    }

    @GetMapping("/ledger")
    public List<ToolExecutionRecord> ledger() {
        return ledger.records();
    }

    @PostMapping("/live-agent")
    public LiveToolAgentResponse liveAgent(@RequestBody LiveToolAgentRequest request) {
        RealAiRuntime.requireConfigured(apiKey, "ai-tool-demo");
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("ai-tool-demo requires a Spring AI ChatClient.Builder bean");
        }
        String content = builder.build()
                .prompt()
                .system("你是企业工单系统里的 Agent。必须优先使用提供的 Tool 查询工单，写操作不能绕过人工确认。")
                .user(request.question())
                .toolCallbacks(new SpringAiToolCallbackBridge(ticketToolFacade).toolCallbackProvider())
                .call()
                .content();
        return new LiveToolAgentResponse("model:" + modelName, modelName, content, ledger.records());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    public record TicketLookupRequest(String ticketId, OperatorContext operator) {
    }

    public record TicketCloseRequest(String ticketId,
                                     boolean humanApproved,
                                     String confirmationToken,
                                     OperatorContext operator) {
    }

    public record LiveToolAgentRequest(String question) {
    }

    public record LiveToolAgentResponse(
            String mode,
            String model,
            String content,
            List<ToolExecutionRecord> toolRecords
    ) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
