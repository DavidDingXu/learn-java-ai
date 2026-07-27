package com.xiaoding.javaai.eval.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentEvaluationReportWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writes_redacted_machine_and_reader_reports() throws Exception {
        AgentEvaluationReport report = new AgentEvaluationReport(
                "agent-v1", "commit-1", "run-1", Instant.parse("2026-07-13T08:00:00Z"),
                1, 0, true,
                List.of(new AgentCaseReport(
                        "assign", true,
                        "WAITING_CONFIRMATION", "WAITING_CONFIRMATION",
                        "ASSIGN_QUEUE", "ASSIGN_QUEUE",
                        "MEDIUM", "MEDIUM",
                        "TICKET_OPERATOR", "TICKET_OPERATOR",
                        Map.of("queueCode", "refund-review"),
                        Map.of("queueCode", "refund-review"),
                        List.of("CONFIRMATION_REQUESTED"), 25, List.of())));
        Path json = tempDir.resolve("agent.json");
        Path markdown = tempDir.resolve("agent.md");

        new AgentEvaluationReportWriter().write(report, json, markdown);

        assertTrue(Files.readString(json).contains("\"passed\" : true"));
        assertTrue(Files.readString(json).contains("\"runId\" : \"run-1\""));
        assertTrue(Files.readString(markdown).contains("ASSIGN_QUEUE"));
        assertFalse(Files.readString(markdown).contains("create-token"));
    }
}
