package com.xiaoding.javaai.eval.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AgentEvalDatasetLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentEvalDataset load(Path path) {
        List<AgentEvalCase> cases = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        String version = null;
        try {
            List<String> lines = Files.readAllLines(path);
            for (int index = 0; index < lines.size(); index += 1) {
                String line = lines.get(index).trim();
                if (line.isEmpty()) continue;
                DatasetLine parsed = objectMapper.readValue(line, DatasetLine.class);
                AgentEvalCase evalCase = parsed.toCase(index + 1);
                if (!ids.add(evalCase.id())) {
                    throw new IllegalArgumentException("duplicate case id: " + evalCase.id());
                }
                if (version == null) version = requireText(parsed.datasetVersion(), "datasetVersion", index + 1);
                if (!version.equals(parsed.datasetVersion())) {
                    throw new IllegalArgumentException("mixed dataset versions at line " + (index + 1));
                }
                cases.add(evalCase);
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("failed to load agent eval dataset: " + path, error);
        }
        return new AgentEvalDataset(version, cases);
    }

    private static String requireText(String value, String name, int line) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + name + " at line " + line);
        }
        return value.trim();
    }

    private record DatasetLine(
            String datasetVersion,
            String id,
            String objective,
            Map<String, String> businessContext,
            String expectedState,
            String expectedTool,
            String expectedRisk,
            String expectedRole,
            List<String> forbiddenAuditEvents,
            List<String> forbiddenAuditFragments
    ) {
        AgentEvalCase toCase(int line) {
            requireText(datasetVersion, "datasetVersion", line);
            return new AgentEvalCase(
                    id, objective, businessContext, expectedState,
                    expectedTool, expectedRisk, expectedRole,
                    forbiddenAuditEvents, forbiddenAuditFragments);
        }
    }
}
