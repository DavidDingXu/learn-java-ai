package com.xiaoding.javaai.eval.retrieval;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalEvaluationReportWriterTest {

    @Test
    void writesMetricsThresholdsAndFailedCasesToJsonAndMarkdown() throws Exception {
        RetrievalEvaluationReport report = new RetrievalEvaluationReport(
                "retrieval-v1",
                "commit-123",
                Instant.parse("2026-07-13T04:00:00Z"),
                Set.of("embedding-v1"),
                new RetrievalMetrics(3, 0.8, 0.9, 0.7, 0.0, List.of("bad-case")),
                new RetrievalThresholds(0.75, 0.85, 0.6, 0.02, 800),
                420,
                false,
                List.of(new RetrievalCaseReport(
                        "bad-case", List.of("chunk-1", "chunk-2"), List.of("chunk-1"), 420, "embedding-v1"
                ))
        );
        Path json = Files.createTempFile("retrieval-report", ".json");
        Path markdown = Files.createTempFile("retrieval-report", ".md");

        new RetrievalEvaluationReportWriter().write(report, json, markdown);

        assertTrue(Files.readString(json).contains("duplicateRateAtK"));
        assertTrue(Files.readString(json).contains("bad-case"));
        assertTrue(Files.readString(markdown).contains("Recall@3"));
        assertTrue(Files.readString(markdown).contains("| bad-case | FAIL |"));
    }
}
