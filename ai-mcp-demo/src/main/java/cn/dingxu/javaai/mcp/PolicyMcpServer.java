package cn.dingxu.javaai.mcp;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PolicyMcpServer {

    private final String serverName;
    private final List<PolicyDocument> documents;

    public PolicyMcpServer(String serverName, List<PolicyDocument> documents) {
        this.serverName = serverName == null || serverName.isBlank() ? "policy-center-mcp" : serverName;
        this.documents = documents == null ? List.of() : List.copyOf(documents);
    }

    public static PolicyMcpServer seeded() {
        return new PolicyMcpServer("policy-center-mcp", List.of(
                new PolicyDocument(
                        "refund-policy-001",
                        "policy://refund/refund-policy-001",
                        "tenant-a",
                        Set.of("support"),
                        "发货后退款需先核对物流状态，确认客户是否拒收、退回或已签收。"
                ),
                new PolicyDocument(
                        "refund-policy-002",
                        "policy://refund/refund-policy-002",
                        "tenant-a",
                        Set.of("support", "ops"),
                        "高金额退款必须转人工复核，系统只能生成建议，不能直接执行退款。"
                ),
                new PolicyDocument(
                        "hr-policy-001",
                        "policy://hr/hr-policy-001",
                        "tenant-a",
                        Set.of("hr"),
                        "薪资制度仅 HR 部门可见。"
                )
        ));
    }

    public McpServerManifest manifest() {
        return new McpServerManifest(
                serverName,
                List.of(
                        new McpToolDescriptor("policy.search", "Search accessible policy documents.", Map.of("query", "string")),
                        new McpToolDescriptor("policy.get", "Read one accessible policy document by documentId.", Map.of("documentId", "string"))
                ),
                documents.stream()
                        .map(document -> new McpResourceDescriptor(document.uri(), document.documentId(), "text/plain"))
                        .toList(),
                List.of(new McpPromptDescriptor(
                        "ticket-policy-answer",
                        "Compose ticket answer with policy evidence.",
                        List.of("ticketSummary", "policyEvidence")
                ))
        );
    }

    public McpToolResult callTool(String toolName, Map<String, Object> arguments, OperatorContext operator) {
        return switch (toolName) {
            case "policy.search" -> searchPolicy(arguments, operator);
            case "policy.get" -> getPolicy(arguments, operator);
            default -> McpToolResult.failed("Unknown tool: " + toolName, Map.of());
        };
    }

    public McpResourceContent readResource(String uri, OperatorContext operator) {
        return documents.stream()
                .filter(document -> Objects.equals(document.uri(), uri))
                .findFirst()
                .map(document -> document.accessibleBy(operator)
                        ? McpResourceContent.visible(uri, document.content())
                        : McpResourceContent.hidden(uri))
                .orElseGet(() -> McpResourceContent.hidden(uri));
    }

    public McpPromptMessage renderPrompt(String name, Map<String, Object> arguments) {
        if (!"ticket-policy-answer".equals(name)) {
            throw new IllegalArgumentException("unknown prompt: " + name);
        }
        Object ticketSummary = arguments.getOrDefault("ticketSummary", "");
        Object policyEvidence = arguments.getOrDefault("policyEvidence", List.of());
        return new McpPromptMessage(
                "你是企业工单助手，只能基于给定制度依据生成建议；依据不足时说明需要人工复核。",
                "工单摘要：\n" + ticketSummary + "\n\n制度依据：\n" + formatEvidence(policyEvidence)
        );
    }

    private McpToolResult searchPolicy(Map<String, Object> arguments, OperatorContext operator) {
        String query = String.valueOf(arguments.getOrDefault("query", "")).trim();
        if (query.isBlank()) {
            return McpToolResult.failed("query must not be blank", Map.of());
        }

        List<Map<String, Object>> matches = documents.stream()
                .filter(document -> document.accessibleBy(operator))
                .filter(document -> document.content().contains(query) || document.documentId().contains(query))
                .sorted(Comparator.comparing(PolicyDocument::documentId))
                .map(document -> Map.<String, Object>of(
                        "documentId", document.documentId(),
                        "uri", document.uri(),
                        "snippet", document.content()
                ))
                .toList();

        return McpToolResult.ok("policy search completed", Map.of("matches", matches));
    }

    private McpToolResult getPolicy(Map<String, Object> arguments, OperatorContext operator) {
        String documentId = String.valueOf(arguments.getOrDefault("documentId", "")).trim();
        if (documentId.isBlank()) {
            return McpToolResult.failed("documentId must not be blank", Map.of());
        }

        return documents.stream()
                .filter(document -> document.documentId().equals(documentId))
                .findFirst()
                .map(document -> document.accessibleBy(operator)
                        ? McpToolResult.ok("policy document loaded", Map.of(
                                "documentId", document.documentId(),
                                "uri", document.uri(),
                                "content", document.content()
                        ))
                        : McpToolResult.failed("policy document is not visible to current operator", Map.of("documentId", documentId)))
                .orElseGet(() -> McpToolResult.failed("policy document not found", Map.of("documentId", documentId)));
    }

    private String formatEvidence(Object evidence) {
        if (evidence instanceof Iterable<?> values) {
            StringBuilder builder = new StringBuilder();
            for (Object value : values) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append("- ").append(value);
            }
            return builder.toString();
        }
        return String.valueOf(evidence);
    }
}
