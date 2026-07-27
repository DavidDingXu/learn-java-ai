package com.xiaoding.javaai.eval.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentEvaluationReportWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void write(AgentEvaluationReport report, Path jsonPath, Path markdownPath) {
        try {
            createParent(jsonPath);
            createParent(markdownPath);
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("datasetVersion", report.datasetVersion());
            json.put("commit", report.commit());
            json.put("runId", report.runId());
            json.put("executedAt", report.executedAt().toString());
            json.put("passedCount", report.passedCount());
            json.put("failedCount", report.failedCount());
            json.put("passed", report.passed());
            json.put("cases", report.cases());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), json);
            Files.writeString(markdownPath, markdown(report));
        } catch (IOException error) {
            throw new IllegalStateException("failed to write agent evaluation report", error);
        }
    }

    private static String markdown(AgentEvaluationReport report) {
        StringBuilder rows = new StringBuilder();
        for (AgentCaseReport evalCase : report.cases()) {
            rows.append("| ").append(evalCase.caseId())
                    .append(" | ").append(evalCase.passed() ? "PASS" : "FAIL")
                    .append(" | ").append(value(evalCase.actualState()))
                    .append(" | ").append(value(evalCase.actualTool()))
                    .append(" | ").append(value(evalCase.actualRisk()))
                    .append(" | ").append(value(evalCase.actualRole()))
                    .append(" | ").append(value(evalCase.actualArguments()))
                    .append(" | ").append(evalCase.latencyMillis())
                    .append(" | ").append(String.join("; ", evalCase.reasons()))
                    .append(" |\n");
        }
        return ("""
                # Agent Evaluation

                - Dataset: `%s`
                - Commit: `%s`
                - Run ID: `%s`
                - Executed at: `%s`
                - Result: **%s**
                - Passed: %d
                - Failed: %d

                | Case | Result | State | Tool | Risk | Required role | Arguments | Latency ms | Reason |
                |---|---:|---|---|---|---|---|---:|---|
                %s
                """.formatted(
                report.datasetVersion(), report.commit(), report.runId(), report.executedAt(),
                report.passed() ? "PASS" : "FAIL",
                report.passedCount(), report.failedCount(), rows)).stripTrailing()
                + System.lineSeparator();
    }

    private static String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String value(Map<String, String> value) {
        if (value == null || value.isEmpty()) return "-";
        return value.toString().replace("|", "\\|").replaceAll("\\s+", " ");
    }

    private static void createParent(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
    }
}
