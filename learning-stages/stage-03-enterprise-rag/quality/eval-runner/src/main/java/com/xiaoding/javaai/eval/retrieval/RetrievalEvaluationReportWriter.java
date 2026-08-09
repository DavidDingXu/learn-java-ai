package com.xiaoding.javaai.eval.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RetrievalEvaluationReportWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void write(RetrievalEvaluationReport report, Path jsonPath, Path markdownPath) {
        try {
            createParent(jsonPath);
            createParent(markdownPath);
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("datasetVersion", report.datasetVersion());
            json.put("commit", report.commit());
            json.put("executedAt", report.executedAt().toString());
            json.put("embeddingModels", report.embeddingModels());
            json.put("metrics", report.metrics());
            json.put("thresholds", report.thresholds());
            json.put("p95LatencyMillis", report.p95LatencyMillis());
            json.put("passed", report.passed());
            json.put("cases", report.cases());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), json);
            Files.writeString(markdownPath, markdown(report));
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write retrieval evaluation report", exception);
        }
    }

    private static String markdown(RetrievalEvaluationReport report) {
        StringBuilder cases = new StringBuilder();
        for (RetrievalCaseReport evalCase : report.cases()) {
            String result = evalCase.status() == RetrievalCaseStatus.ERROR
                    ? "ERROR"
                    : evalCase.retrievedChunkIds().containsAll(evalCase.expectedChunkIds()) ? "PASS" : "FAIL";
            cases.append("| ").append(evalCase.caseId())
                    .append(" | ").append(result)
                    .append(" | ").append(evalCase.latencyMillis())
                    .append(" | ").append(String.join(", ", evalCase.expectedChunkIds()))
                    .append(" | ").append(String.join(", ", evalCase.retrievedChunkIds()))
                    .append(" | ").append(markdownCell(evalCase.error()))
                    .append(" |\n");
        }
        RetrievalMetrics metrics = report.metrics();
        RetrievalThresholds thresholds = report.thresholds();
        return ("""
                # Retrieval Evaluation

                - Dataset: `%s`
                - Commit: `%s`
                - Executed at: `%s`
                - Embedding models: `%s`
                - Result: **%s**
                - Recall@%d: %.4f (minimum %.4f)
                - HitRate@%d: %.4f (minimum %.4f)
                - MRR: %.4f (minimum %.4f)
                - DuplicateRate@%d: %.4f (maximum %.4f)
                - P95 latency: %d ms (maximum %d ms)

                | Case | Result | Latency ms | Expected chunks | Retrieved chunks | Error |
                |---|---:|---:|---|---|---|
                %s
                """.formatted(
                report.datasetVersion(), report.commit(), report.executedAt(),
                String.join(", ", report.embeddingModels()), report.passed() ? "PASS" : "FAIL",
                metrics.k(), metrics.recallAtK(), thresholds.minimumRecallAtK(),
                metrics.k(), metrics.hitRateAtK(), thresholds.minimumHitRateAtK(),
                metrics.meanReciprocalRank(), thresholds.minimumMeanReciprocalRank(),
                metrics.k(), metrics.duplicateRateAtK(), thresholds.maximumDuplicateRateAtK(),
                report.p95LatencyMillis(), thresholds.maximumP95LatencyMillis(), cases
        )).stripTrailing() + System.lineSeparator();
    }

    private static String markdownCell(String value) {
        return value == null ? "" : value.replace("|", "\\|").replaceAll("[\\r\\n]+", " ");
    }

    private static void createParent(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
    }
}
