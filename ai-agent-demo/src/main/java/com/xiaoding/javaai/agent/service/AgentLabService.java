package com.xiaoding.javaai.agent.service;

import com.xiaoding.javaai.agent.service.context.ContextBudget;
import com.xiaoding.javaai.agent.service.context.ContextEngineeringService;
import com.xiaoding.javaai.agent.service.context.ContextSlice;
import com.xiaoding.javaai.agent.service.context.EngineeredContext;
import com.xiaoding.javaai.agent.service.memory.AgentContext;
import com.xiaoding.javaai.agent.service.memory.AgentContextAssembler;
import com.xiaoding.javaai.agent.service.memory.BusinessSnapshot;
import com.xiaoding.javaai.agent.service.memory.ConversationMemoryStore;
import com.xiaoding.javaai.agent.service.memory.MemoryEntry;
import com.xiaoding.javaai.agent.service.memory.MemoryEntryType;
import com.xiaoding.javaai.agent.service.memory.MemoryScope;
import com.xiaoding.javaai.agent.service.memory.UserPreference;
import com.xiaoding.javaai.agent.service.react.GuardedReActAgent;
import com.xiaoding.javaai.agent.service.react.ReActAction;
import com.xiaoding.javaai.agent.service.react.ReActPolicy;
import com.xiaoding.javaai.agent.service.react.ReActRunResult;
import com.xiaoding.javaai.agent.service.react.ScriptedReActPlanner;
import com.xiaoding.javaai.agent.service.react.hook.AgentHookChain;
import com.xiaoding.javaai.agent.service.react.hook.PiiMaskingAgentHook;
import com.xiaoding.javaai.agent.service.react.hook.ToolAllowlistAgentHook;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentLabService {

    private static final String DEFAULT_TENANT_ID = "tenant-a";
    private static final String DEFAULT_SESSION_ID = "s-lab";
    private static final String DEFAULT_BUSINESS_ID = "T-1001";
    private static final String DEFAULT_USER_ID = "u1001";

    private final AgentContextAssembler contextAssembler;
    private final ConversationMemoryStore memoryStore;
    private final ContextEngineeringService contextEngineeringService;
    private final Set<String> seededContextKeys = ConcurrentHashMap.newKeySet();

    public AgentLabService(AgentContextAssembler contextAssembler,
                           ConversationMemoryStore memoryStore,
                           ContextEngineeringService contextEngineeringService) {
        this.contextAssembler = contextAssembler;
        this.memoryStore = memoryStore;
        this.contextEngineeringService = contextEngineeringService;
    }

    public ContextLabResult context(ContextLabRequest request) {
        String tenantId = valueOrDefault(request.tenantId(), DEFAULT_TENANT_ID);
        String sessionId = valueOrDefault(request.sessionId(), DEFAULT_SESSION_ID);
        String businessId = valueOrDefault(request.businessId(), DEFAULT_BUSINESS_ID);
        String userId = valueOrDefault(request.userId(), DEFAULT_USER_ID);
        seedMemory(tenantId, sessionId, businessId, userId);

        AgentContext context = contextAssembler.assemble(tenantId, sessionId, businessId, userId);
        ContextBudget budget = new ContextBudget(
                request.maxEstimatedTokens() == null ? 180 : request.maxEstimatedTokens(),
                request.maxConversationMessages() == null ? 4 : request.maxConversationMessages()
        );
        EngineeredContext engineered = contextEngineeringService.assemble(context, budget);
        return new ContextLabResult(
                new ContextScopeView(tenantId, sessionId, businessId, userId),
                budget,
                engineered.totalEstimatedTokens(),
                engineered.sources(),
                engineered.slices().stream().map(this::sliceView).toList()
        );
    }

    public ReActRunResult react(ReActLabRequest request) {
        String scenario = valueOrDefault(request.scenario(), "normal");
        List<ReActAction> actions = switch (scenario) {
            case "pii" -> List.of(
                    ReActAction.callTool("lookupTicket", "读取工单，联系人手机号 13812345678"),
                    ReActAction.callTool("lookupOrder", "读取订单状态"),
                    ReActAction.callTool("retrievePolicy", "检索发货后退款制度"),
                    ReActAction.finish("建议先核对物流状态，高金额退款转人工复核。")
            );
            case "blockedTool" -> List.of(
                    ReActAction.callTool("refundDirectly", "直接执行退款"),
                    ReActAction.finish("不应该执行到这里。")
            );
            default -> List.of(
                    ReActAction.callTool("lookupTicket", "读取工单"),
                    ReActAction.callTool("lookupOrder", "读取订单状态"),
                    ReActAction.callTool("retrievePolicy", "检索发货后退款制度"),
                    ReActAction.finish("建议先核对物流状态，高金额退款转人工复核。")
            );
        };
        int maxSteps = request.maxSteps() == null ? 5 : Math.max(1, request.maxSteps());
        Set<String> allowedTools = Set.of("lookupTicket", "lookupOrder", "retrievePolicy");
        GuardedReActAgent agent = new GuardedReActAgent(
                new ScriptedReActPlanner(actions),
                ReActPolicy.readOnly(allowedTools, maxSteps),
                new AgentHookChain(List.of(
                        new PiiMaskingAgentHook(),
                        new ToolAllowlistAgentHook(allowedTools)
                ))
        );
        return agent.run(valueOrDefault(request.userInput(), "客户申请退款但订单已经发货，能直接关闭工单吗？"));
    }

    private void seedMemory(String tenantId, String sessionId, String businessId, String userId) {
        String seedKey = String.join("|", tenantId, sessionId, businessId, userId);
        if (!seededContextKeys.add(seedKey)) {
            return;
        }
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        memoryStore.append(new MemoryEntry(
                tenantId,
                sessionId,
                userId,
                MemoryEntryType.USER_MESSAGE,
                "客户说商品已经发货，但仍然要求退款。",
                Map.of("turn", 1),
                base
        ));
        memoryStore.append(new MemoryEntry(
                tenantId,
                sessionId,
                userId,
                MemoryEntryType.TOOL_OBSERVATION,
                "订单状态 SHIPPED，金额 5000，物流单号已生成。",
                Map.of("tool", "order.lookup"),
                base.plusSeconds(60)
        ));
        memoryStore.append(new MemoryEntry(
                tenantId,
                sessionId,
                userId,
                MemoryEntryType.AGENT_MESSAGE,
                "已提示先核对物流状态，并标记为高金额风险。",
                Map.of("turn", 2),
                base.plusSeconds(120)
        ));
        memoryStore.append(new MemoryEntry(
                tenantId,
                sessionId,
                userId,
                MemoryEntryType.USER_MESSAGE,
                "用户又补充说不接受转人工等待。",
                Map.of("turn", 3),
                base.plusSeconds(180)
        ));
        memoryStore.saveBusinessSnapshot(new BusinessSnapshot(
                tenantId,
                sessionId,
                businessId,
                Map.of("ticketStatus", "OPEN", "orderStatus", "SHIPPED", "amount", 5000),
                base.plusSeconds(240)
        ));
        memoryStore.savePreference(new UserPreference(
                tenantId,
                userId,
                MemoryScope.LONG_TERM,
                "tone",
                "先给明确结论，再解释原因",
                base.plusSeconds(300)
        ));
    }

    private ContextSliceView sliceView(ContextSlice slice) {
        return new ContextSliceView(
                slice.type().name(),
                slice.source(),
                slice.content(),
                slice.estimatedTokens(),
                slice.attributes()
        );
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record ContextLabRequest(
            String tenantId,
            String sessionId,
            String businessId,
            String userId,
            Integer maxEstimatedTokens,
            Integer maxConversationMessages
    ) {
    }

    public record ContextLabResult(
            ContextScopeView scope,
            ContextBudget budget,
            int totalEstimatedTokens,
            List<String> sources,
            List<ContextSliceView> slices
    ) {
    }

    public record ReActLabRequest(
            String scenario,
            String userInput,
            Integer maxSteps
    ) {
    }

    public record ContextScopeView(
            String tenantId,
            String sessionId,
            String businessId,
            String userId
    ) {
    }

    public record ContextSliceView(
            String type,
            String source,
            String content,
            int estimatedTokens,
            Map<String, Object> attributes
    ) {
    }
}
