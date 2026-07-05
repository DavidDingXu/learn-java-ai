package com.xiaoding.javaai.mcp;

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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/mcp")
public class McpDemoController {

    private final PolicyMcpServer server = PolicyMcpServer.seeded();
    private final McpAuditLedger ledger = new McpAuditLedger();
    private final ToolAccessPolicy accessPolicy = ToolAccessPolicy.builder()
            .allowTool("policy.search")
            .allowTool("policy.get")
            .requirePermission("policy.get", "policy:read-detail")
            .build();
    private final McpRemoteEndpoint endpoint = new McpRemoteEndpoint(
            "https://example.local/mcp/policy-center",
            McpTransportType.STREAMABLE_HTTP,
            server,
            Duration.ofSeconds(3)
    );
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public McpDemoController(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                             @Value("${spring.ai.openai.api-key:}") String apiKey,
                             @Value("${java-ai.mcp.model-name:gpt-4o-mini}") String modelName) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @GetMapping("/session")
    public McpClientSession session() {
        return hostClient().initialize("helpdesk-agent-host");
    }

    @PostMapping("/tools/call")
    public McpToolResult callTool(@RequestBody ToolCallRequest request) {
        return hostClient().callTool(request.toolName(), request.arguments(), request.operator().toOperatorContext());
    }

    @PostMapping("/resources/read")
    public McpResourceContent readResource(@RequestBody ResourceReadRequest request) {
        return server.readResource(request.uri(), request.operator().toOperatorContext());
    }

    @GetMapping("/debug")
    public McpDebugReport debug() {
        return remoteClient().debugReport();
    }

    @GetMapping("/audit")
    public java.util.List<McpAuditRecord> audit() {
        return ledger.records();
    }

    @PostMapping("/live-answer")
    public McpLiveAnswer liveAnswer(@RequestBody McpLiveAnswerRequest request) {
        OperatorContext operator = request.operator().toOperatorContext();
        McpClientSession session = hostClient().initialize("helpdesk-agent-host");
        McpToolResult searchResult = hostClient().callTool(
                "policy.search",
                Map.of("query", request.query()),
                operator
        );
        Object matches = searchResult.data().getOrDefault("matches", List.of());
        McpPromptMessage prompt = server.renderPrompt("ticket-policy-answer", Map.of(
                "ticketSummary", request.ticketSummary(),
                "policyEvidence", matches
        ));
        LiveAiResult answer = new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                modelName,
                "ai-mcp-demo"
        ).call(prompt.system(), prompt.user());
        return new McpLiveAnswer(session, searchResult, prompt, answer);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    private McpHostClient hostClient() {
        return new McpHostClient(server, accessPolicy, ledger);
    }

    private McpRemoteHostClient remoteClient() {
        return new McpRemoteHostClient("helpdesk-agent-host", endpoint, accessPolicy, ledger);
    }

    public record ToolCallRequest(
            String toolName,
            Map<String, Object> arguments,
            OperatorView operator
    ) {
        public ToolCallRequest {
            arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
            operator = operator == null ? OperatorView.defaultSupport() : operator;
        }
    }

    public record ResourceReadRequest(
            String uri,
            OperatorView operator
    ) {
        public ResourceReadRequest {
            operator = operator == null ? OperatorView.defaultSupport() : operator;
        }
    }

    public record OperatorView(
            String userId,
            String tenantId,
            String department,
            Set<String> permissions
    ) {
        public static OperatorView defaultSupport() {
            return new OperatorView("u1001", "tenant-a", "support", Set.of());
        }

        public OperatorContext toOperatorContext() {
            return new OperatorContext(
                    valueOrDefault(userId, "u1001"),
                    valueOrDefault(tenantId, "tenant-a"),
                    valueOrDefault(department, "support"),
                    permissions == null ? Set.of() : permissions
            );
        }

        private String valueOrDefault(String value, String defaultValue) {
            return value == null || value.isBlank() ? defaultValue : value;
        }
    }

    public record McpLiveAnswerRequest(
            String ticketSummary,
            String query,
            OperatorView operator
    ) {
        public McpLiveAnswerRequest {
            ticketSummary = ticketSummary == null || ticketSummary.isBlank()
                    ? "客户申请退款，但订单已经发货。"
                    : ticketSummary;
            query = query == null || query.isBlank() ? "退款" : query;
            operator = operator == null ? OperatorView.defaultSupport() : operator;
        }
    }

    public record McpLiveAnswer(
            McpClientSession session,
            McpToolResult toolResult,
            McpPromptMessage prompt,
            LiveAiResult answer
    ) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
