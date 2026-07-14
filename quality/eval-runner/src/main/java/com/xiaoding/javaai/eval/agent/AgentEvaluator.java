package com.xiaoding.javaai.eval.agent;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class AgentEvaluator {

    private final AgentEvaluationClient client;
    private final Clock clock;

    public AgentEvaluator(AgentEvaluationClient client, Clock clock) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public AgentEvaluationReport evaluate(
            AgentEvalDataset dataset,
            URI baseUrl,
            AgentEvaluationTokens tokens,
            String commit
    ) {
        Objects.requireNonNull(dataset, "dataset must not be null");
        Objects.requireNonNull(tokens, "tokens must not be null");
        validateBaseUrl(baseUrl);
        String normalizedCommit = requireText(commit, "commit");
        List<AgentCaseReport> reports = new ArrayList<>();
        for (AgentEvalCase evalCase : dataset.cases()) {
            try {
                AgentEvaluationSnapshot snapshot = client.evaluate(
                        baseUrl, tokens, evalCase, idempotencyKey(evalCase.id(), normalizedCommit));
                reports.add(compare(evalCase, snapshot));
            } catch (RuntimeException error) {
                reports.add(new AgentCaseReport(
                        evalCase.id(), false,
                        evalCase.expectedState(), null,
                        evalCase.expectedTool(), null,
                        evalCase.expectedRisk(), null,
                        evalCase.expectedRole(), null,
                        List.of(), 0,
                        List.of("CLIENT_ERROR: " + error.getMessage())));
            }
        }
        int passed = (int) reports.stream().filter(AgentCaseReport::passed).count();
        return new AgentEvaluationReport(
                dataset.version(), normalizedCommit, Instant.now(clock),
                passed, reports.size() - passed, passed == reports.size(), reports);
    }

    private static AgentCaseReport compare(AgentEvalCase expected, AgentEvaluationSnapshot actual) {
        List<String> reasons = new ArrayList<>();
        compare("state", expected.expectedState(), actual.state(), reasons);
        compare("tool", expected.expectedTool(), actual.toolName(), reasons);
        compare("risk", expected.expectedRisk(), actual.risk(), reasons);
        compare("role", expected.expectedRole(), actual.requiredRole(), reasons);
        expected.forbiddenAuditEvents().stream()
                .filter(actual.auditEventTypes()::contains)
                .forEach(event -> reasons.add("forbidden audit event: " + event));
        expected.forbiddenAuditFragments().stream()
                .filter(fragment -> actual.auditDetails().stream().anyMatch(detail -> detail.contains(fragment)))
                .forEach(fragment -> reasons.add("forbidden audit fragment present"));
        return new AgentCaseReport(
                expected.id(), reasons.isEmpty(),
                expected.expectedState(), actual.state(),
                expected.expectedTool(), actual.toolName(),
                expected.expectedRisk(), actual.risk(),
                expected.expectedRole(), actual.requiredRole(),
                actual.auditEventTypes(), actual.latencyMillis(), reasons);
    }

    private static void compare(String field, String expected, String actual, List<String> reasons) {
        if (!Objects.equals(expected, actual)) {
            reasons.add(field + " expected=" + expected + " actual=" + actual);
        }
    }

    private static String idempotencyKey(String caseId, String commit) {
        String canonical = caseId + "\n" + commit;
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
            String safeCase = caseId.replaceAll("[^A-Za-z0-9._:-]", "-");
            if (safeCase.length() > 40) safeCase = safeCase.substring(0, 40);
            return "agent-eval:" + safeCase + ":" + hash.substring(0, 16);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static void validateBaseUrl(URI baseUrl) {
        if (baseUrl == null || !baseUrl.isAbsolute()
                || !("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))) {
            throw new IllegalArgumentException("baseUrl must be an absolute HTTP(S) URI");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
