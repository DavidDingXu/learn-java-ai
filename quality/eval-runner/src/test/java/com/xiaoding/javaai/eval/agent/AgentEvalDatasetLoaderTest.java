package com.xiaoding.javaai.eval.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentEvalDatasetLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loads_versioned_agent_expectations() throws Exception {
        Path dataset = tempDir.resolve("agent.jsonl");
        Files.writeString(dataset, """
                {"datasetVersion":"agent-v1","id":"assign","objective":"assign ticket","businessContext":{"queueCode":"refund-review"},"expectedState":"WAITING_CONFIRMATION","expectedTool":"ASSIGN_QUEUE","expectedRisk":"MEDIUM","expectedRole":"TICKET_OPERATOR","forbiddenAuditEvents":["TOOL_EXECUTION_SUCCEEDED"],"forbiddenAuditFragments":["13800138000"]}
                """);

        AgentEvalDataset loaded = new AgentEvalDatasetLoader().load(dataset);

        assertEquals("agent-v1", loaded.version());
        assertEquals("ASSIGN_QUEUE", loaded.cases().get(0).expectedTool());
        assertEquals("13800138000", loaded.cases().get(0).forbiddenAuditFragments().get(0));
    }

    @Test
    void rejects_duplicate_case_ids() throws Exception {
        Path dataset = tempDir.resolve("duplicate.jsonl");
        String line = """
                {"datasetVersion":"agent-v1","id":"same","objective":"assign ticket","businessContext":{},"expectedState":"REJECTED","expectedTool":null,"expectedRisk":null,"expectedRole":null,"forbiddenAuditEvents":[],"forbiddenAuditFragments":[]}
                """.trim();
        Files.writeString(dataset, line + System.lineSeparator() + line);

        assertThrows(IllegalArgumentException.class,
                () -> new AgentEvalDatasetLoader().load(dataset));
    }
}
