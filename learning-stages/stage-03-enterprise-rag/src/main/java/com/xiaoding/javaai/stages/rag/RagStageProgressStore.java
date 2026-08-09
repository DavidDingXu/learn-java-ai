package com.xiaoding.javaai.stages.rag;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class RagStageProgressStore {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path path;

    RagStageProgressStore(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    RagStageProgress load() {
        if (!Files.isRegularFile(path)) return RagStageProgress.empty();
        try {
            return JSON.readValue(path.toFile(), RagStageProgress.class);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read RAG stage state " + path, error);
        }
    }

    void save(RagStageProgress progress) {
        try {
            Files.createDirectories(path.getParent());
            JSON.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), progress);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot write RAG stage state " + path, error);
        }
    }
}
