package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;
import com.xiaoding.javaai.ticket.agent.domain.PreparedToolCall;
import com.xiaoding.javaai.ticket.agent.domain.ToolEffect;
import com.xiaoding.javaai.ticket.agent.domain.ToolRisk;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class BusinessToolCatalog {

    private final Map<String, ToolPolicy> policies;
    private final Set<String> allowedQueues;

    private BusinessToolCatalog(Map<String, ToolPolicy> policies, Set<String> allowedQueues) {
        this.policies = Map.copyOf(policies);
        this.allowedQueues = Set.copyOf(allowedQueues);
    }

    public static BusinessToolCatalog standard(Set<String> allowedQueues) {
        return new BusinessToolCatalog(Map.of(
                "QUERY_KNOWLEDGE", new ToolPolicy(
                        ToolEffect.READ, ToolRisk.READ_ONLY, "", Set.of("question")),
                "ADD_INTERNAL_NOTE", new ToolPolicy(
                        ToolEffect.WRITE, ToolRisk.LOW, "TICKET_OPERATOR", Set.of("note")),
                "ASSIGN_QUEUE", new ToolPolicy(
                        ToolEffect.WRITE, ToolRisk.MEDIUM, "TICKET_OPERATOR", Set.of("queueCode")),
                "ISSUE_REFUND", new ToolPolicy(
                        ToolEffect.WRITE, ToolRisk.HIGH, "REFUND_APPROVER", Set.of("amountMinor", "currency")),
                "REQUEST_MANUAL_REVIEW", new ToolPolicy(
                        ToolEffect.WRITE, ToolRisk.LOW, "TICKET_OPERATOR", Set.of("reasonCode"))
        ), allowedQueues);
    }

    public Set<String> toolNames() {
        return policies.keySet();
    }

    public Map<String, Set<String>> toolArgumentNames() {
        Map<String, Set<String>> specifications = new LinkedHashMap<>();
        policies.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> specifications.put(
                        entry.getKey(), entry.getValue().allowedArguments()));
        return Map.copyOf(specifications);
    }

    public PreparedToolCall prepare(AgentDecision.UseTool proposal) {
        ToolPolicy policy = policies.get(proposal.toolName());
        if (policy == null) throw new IllegalArgumentException("unknown tool: " + proposal.toolName());
        validateArgumentNames(proposal.arguments(), policy.allowedArguments());
        Map<String, String> arguments = normalizeArguments(proposal.toolName(), proposal.arguments());
        return new PreparedToolCall(
                proposal.toolName(),
                policy.effect(),
                policy.risk(),
                policy.requiredRole(),
                arguments,
                proposal.rationale(),
                ToolActionFingerprint.calculate(proposal.toolName(), arguments));
    }

    private void validateArgumentNames(Map<String, String> arguments, Set<String> allowed) {
        for (String name : arguments.keySet()) {
            if (!allowed.contains(name)) throw new IllegalArgumentException("argument is not allowed: " + name);
        }
        for (String required : allowed) {
            if (!arguments.containsKey(required)) throw new IllegalArgumentException("missing argument: " + required);
        }
    }

    private Map<String, String> normalizeArguments(String toolName, Map<String, String> source) {
        Map<String, String> normalized = new LinkedHashMap<>();
        source.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                normalized.put(entry.getKey(), requireText(entry.getValue(), entry.getKey(), 2000)));
        switch (toolName) {
            case "QUERY_KNOWLEDGE" -> enforceLength(normalized.get("question"), "question", 1000);
            case "ASSIGN_QUEUE" -> {
                String queueCode = normalized.get("queueCode");
                if (!allowedQueues.contains(queueCode)) {
                    throw new IllegalArgumentException("queueCode is not configured: " + queueCode);
                }
            }
            case "ISSUE_REFUND" -> validateRefund(normalized);
            default -> {
            }
        }
        return Map.copyOf(normalized);
    }

    private static void validateRefund(Map<String, String> arguments) {
        String amount = arguments.get("amountMinor");
        if (!amount.matches("[1-9][0-9]{0,9}")) {
            throw new IllegalArgumentException("amountMinor must be a positive integer");
        }
        if (!Set.of("CNY", "USD").contains(arguments.get("currency"))) {
            throw new IllegalArgumentException("currency is not supported");
        }
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        String normalized = value.trim();
        enforceLength(normalized, name, maxLength);
        return normalized;
    }

    private static void enforceLength(String value, String name, int maxLength) {
        if (value.length() > maxLength) throw new IllegalArgumentException(name + " exceeds " + maxLength);
    }

    private record ToolPolicy(
            ToolEffect effect,
            ToolRisk risk,
            String requiredRole,
            Set<String> allowedArguments
    ) {
    }
}
