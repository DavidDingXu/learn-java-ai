package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.permission.PermissionEngine;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.Toolkit;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AgentScopeTicketRuntime {

    private final Toolkit toolkit;
    private final PermissionEngine permissionEngine;

    private AgentScopeTicketRuntime(Toolkit toolkit, PermissionEngine permissionEngine) {
        this.toolkit = toolkit;
        this.permissionEngine = permissionEngine;
    }

    public static AgentScopeTicketRuntime create(Toolkit toolkit, PermissionContextState permissions) {
        return new AgentScopeTicketRuntime(
                Objects.requireNonNull(toolkit, "toolkit must not be null"),
                new PermissionEngine(Objects.requireNonNull(permissions, "permissions must not be null")));
    }

    public static AgentScopeTicketRuntime createDefault(TicketBusinessTools tools) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(Objects.requireNonNull(tools, "tools must not be null"));
        PermissionContextState permissions = PermissionContextState.builder()
                .mode(PermissionMode.DEFAULT)
                .addAllowRule("query_ticket",
                        new PermissionRule("query_ticket", null, PermissionBehavior.ALLOW, "ticket-policy"))
                .addAskRule("update_ticket",
                        new PermissionRule("update_ticket", null, PermissionBehavior.ASK, "ticket-policy"))
                .addDenyRule("export_all_customers",
                        new PermissionRule("export_all_customers", null, PermissionBehavior.DENY, "data-policy"))
                .build();
        return create(toolkit, permissions);
    }

    public ToolAuthorizationDecision authorize(
            AgentExecutionIdentity identity,
            String toolName,
            Map<String, Object> input) {
        Objects.requireNonNull(identity, "identity must not be null");
        AgentTool tool = toolkit.getTool(toolName);
        if (!(tool instanceof ToolBase toolBase)) {
            throw new IllegalArgumentException("unknown or unsupported tool: " + toolName);
        }
        Map<String, Object> toolInput = input == null ? Map.of() : Map.copyOf(input);
        RuntimeContext context = RuntimeContext.builder()
                .sessionId(identity.tenantId() + ":" + identity.subjectId())
                .userId(identity.subjectId())
                .put(AgentExecutionIdentity.class, identity)
                .build();
        PermissionDecision decision = permissionEngine.checkPermission(toolBase, toolInput).block();
        if (decision == null) {
            throw new IllegalStateException("permission engine produced no decision");
        }
        AgentExecutionIdentity trustedIdentity = context.get(AgentExecutionIdentity.class);
        return new ToolAuthorizationDecision(
                trustedIdentity.tenantId(),
                trustedIdentity.subjectId(),
                toolName,
                decision.getBehavior(),
                matchingRuleSource(toolBase, toolInput, decision.getBehavior()),
                normalizedReason(decision));
    }

    private String matchingRuleSource(
            ToolBase tool,
            Map<String, Object> input,
            PermissionBehavior behavior) {
        Map<String, List<PermissionRule>> rules = switch (behavior) {
            case ALLOW -> permissionEngine.getAllowRules();
            case ASK -> permissionEngine.getAskRules();
            case DENY -> permissionEngine.getDenyRules();
            case PASSTHROUGH -> Map.of();
        };
        return rules.getOrDefault(tool.getName(), List.of()).stream()
                .filter(rule -> tool.matchRule(rule.ruleContent(), input))
                .map(PermissionRule::source)
                .findFirst()
                .orElse("permission-engine-default");
    }

    private static String normalizedReason(PermissionDecision decision) {
        String reason = decision.getDecisionReason();
        return reason == null || "Rule: null".equals(reason) ? decision.getMessage() : reason;
    }
}
