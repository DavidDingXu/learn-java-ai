package com.xiaoding.javaai.eval.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RetrievalEvalDatasetLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RetrievalEvalDataset load(Path path) {
        List<RetrievalEvalCase> cases = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        String version = null;
        try {
            List<String> lines = Files.readAllLines(path);
            for (int index = 0; index < lines.size(); index += 1) {
                String line = lines.get(index).trim();
                if (line.isEmpty()) continue;
                DatasetLine parsed = objectMapper.readValue(line, DatasetLine.class);
                validate(parsed, index + 1);
                if (!ids.add(parsed.id())) {
                    throw new IllegalArgumentException("duplicate case id: " + parsed.id());
                }
                if (version == null) version = parsed.datasetVersion();
                if (!version.equals(parsed.datasetVersion())) {
                    throw new IllegalArgumentException("mixed dataset versions at line " + (index + 1));
                }
                cases.add(new RetrievalEvalCase(parsed.id(), parsed.question(), parsed.expectedChunkIds()));
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("failed to load retrieval eval dataset: " + path, exception);
        }
        if (cases.isEmpty()) throw new IllegalArgumentException("retrieval eval dataset is empty: " + path);
        return new RetrievalEvalDataset(version, cases);
    }

    private static void validate(DatasetLine line, int lineNumber) {
        if (line.datasetVersion() == null || line.datasetVersion().isBlank()) {
            throw new IllegalArgumentException("missing datasetVersion at line " + lineNumber);
        }
        if (line.id() == null || line.id().isBlank()) {
            throw new IllegalArgumentException("missing case id at line " + lineNumber);
        }
        if (line.question() == null || line.question().isBlank()) {
            throw new IllegalArgumentException("missing question at line " + lineNumber);
        }
        if (line.expectedChunkIds() == null || line.expectedChunkIds().isEmpty()) {
            throw new IllegalArgumentException("missing expectedChunkIds at line " + lineNumber);
        }
    }

    private record DatasetLine(
            String datasetVersion,
            String id,
            String question,
            Set<String> expectedChunkIds
    ) {
    }
}
