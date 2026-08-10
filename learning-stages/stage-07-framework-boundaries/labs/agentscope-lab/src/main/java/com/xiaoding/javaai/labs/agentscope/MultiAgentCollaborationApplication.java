package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public final class MultiAgentCollaborationApplication {

    private MultiAgentCollaborationApplication() {
    }

    public static void main(String[] args) {
        try {
            CollaborationDecision decision = new CollaborationPolicy().decide(List.of(
                    new WorkUnit("policy-search", true, false),
                    new WorkUnit("ticket-query", true, false)));
            if (decision.mode() != CollaborationMode.MULTI_AGENT) {
                throw new IllegalStateException("the selected work units are not eligible for multi-agent execution");
            }

            Properties config = AgentScopeLabApplication.loadConfig();
            OpenAIChatModel model = AgentScopeLabApplication.createModel(config);
            Toolkit policyToolkit = new Toolkit();
            policyToolkit.registerTool(new PolicyBusinessTools());
            Toolkit ticketToolkit = new Toolkit();
            ticketToolkit.registerTool(new TicketBusinessTools());

            try (AgentScopeCollaborationAgent policyAgent = AgentScopeCollaborationAgent.specialist(
                    "policy-agent",
                    "你是制度查询专家。必须调用 query_refund_policy，只返回制度中的到账时效。",
                    model,
                    policyToolkit,
                    allow("query_refund_policy", "knowledge-read-policy"));
             AgentScopeCollaborationAgent ticketAgent = AgentScopeCollaborationAgent.specialist(
                    "ticket-agent",
                    "你是工单查询专家。必须调用 query_ticket，只返回工单编号和状态。",
                    model,
                    ticketToolkit,
                    allow("query_ticket", "ticket-read-policy"));
             AgentScopeCollaborationAgent summaryAgent = AgentScopeCollaborationAgent.synthesizer(
                    "summary-agent",
                    "你是客服回复协调者。只合并专家已提供的事实；事实缺失时明确转人工，不补充猜测。",
                    model)) {
                MultiAgentResult result = new MultiAgentCoordinator(Duration.ofSeconds(45)).execute(
                        "工单 T-100 的退款现在是什么状态，预计多久能到账？",
                        List.of(policyAgent, ticketAgent),
                        summaryAgent);
                System.out.printf("decision=%s status=%s specialists=%s answer=%s reason=%s%n",
                        decision.mode(), result.status(), result.specialists(),
                        result.answer(), result.reason());
            }
        } finally {
            Schedulers.shutdownNow();
        }
    }

    private static PermissionContextState allow(String toolName, String source) {
        return PermissionContextState.builder()
                .mode(PermissionMode.DEFAULT)
                .addAllowRule(toolName,
                        new PermissionRule(toolName, null, PermissionBehavior.ALLOW, source))
                .build();
    }
}
