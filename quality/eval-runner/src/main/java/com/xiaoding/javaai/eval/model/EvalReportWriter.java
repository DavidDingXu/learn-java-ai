package com.xiaoding.javaai.eval.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EvalReportWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void write(EvalReport report, Path jsonPath, Path markdownPath) {
        try {
            createParent(jsonPath);
            createParent(markdownPath);
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("datasetVersion", report.datasetVersion());
            json.put("mode", report.mode());
            json.put("commit", report.commit());
            json.put("model", report.model());
            json.put("executedAt", report.executedAt().toString());
            json.put("passed", report.passed());
            json.put("failed", report.failed());
            json.put("skipped", report.skipped());
            json.put("totalTokens", report.totalTokens());
            json.put("results", report.results());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), json);
            Files.writeString(markdownPath, markdown(report));
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write eval report", exception);
        }
    }

    private static String markdown(EvalReport report) {
        StringBuilder cases = new StringBuilder();
        for (EvalCaseResult result : report.results()) {
            cases.append("| ").append(result.caseId())
                    .append(" | ").append(result.passed() ? "PASS" : "FAIL")
                    .append(" | ").append(result.latencyMillis())
                    .append(" | ").append(result.traceId())
                    .append(" | ").append(result.reason().replace("|", "\\|"))
                    .append(" |\n");
        }
        return ("""
                # Model Interaction Evaluation

                - Dataset: `%s`
                - Mode: `%s`
                - Commit: `%s`
                - Model: `%s`
                - Executed at: `%s`
                - Passed: %d
                - Failed: %d
                - Skipped: %d
                - Total tokens: %d

                | Case | Result | Latency ms | Trace ID | Reason |
                |---|---:|---:|---|---|
                %s
                """.formatted(
                report.datasetVersion(), report.mode(), report.commit(), report.model(),
                report.executedAt(), report.passed(), report.failed(), report.skipped(),
                report.totalTokens(), cases
        )).stripTrailing() + System.lineSeparator();
    }

    private static void createParent(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
    }
}
