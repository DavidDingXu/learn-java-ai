package com.xiaoding.javaai.eval.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalReportWriterTest {

    @Test
    void writesMachineReadableAndMarkdownReports() throws Exception {
        EvalReport report = new EvalReport(
                "golden-v1", EvalMode.CONTRACT_FIXTURE, "commit-123", "fixture-model",
                "knowledge-answer-v1", "local-contract-fixture",
                Instant.parse("2026-07-12T12:00:00Z"), 1, 0, 0, 28,
                List.of(new EvalCaseResult("grounded", true, "ok", 42, "trace-1"))
        );
        Path json = Files.createTempFile("eval-report", ".json");
        Path markdown = Files.createTempFile("eval-report", ".md");

        new EvalReportWriter().write(report, json, markdown);

        assertTrue(Files.readString(json).contains("CONTRACT_FIXTURE"));
        assertTrue(Files.readString(json).contains("knowledge-answer-v1"));
        assertTrue(Files.readString(markdown).contains("local-contract-fixture"));
        assertTrue(Files.readString(markdown).contains("golden-v1"));
        assertTrue(Files.readString(markdown).contains("grounded"));
    }
}
