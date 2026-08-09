package com.xiaoding.javaai.stages.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StageConfig {

    private static final Path DEFAULT_PATH = Path.of("learning-stages/config/application.yml");
    private final JsonNode root;

    private StageConfig(JsonNode root) {
        this.root = root;
    }

    public static StageConfig load() {
        Path path = DEFAULT_PATH.toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "Cannot find " + DEFAULT_PATH + ". Set the IDE working directory to the project root."
            );
        }
        try {
            return new StageConfig(new ObjectMapper(new YAMLFactory()).readTree(path.toFile()));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read " + path, error);
        }
    }

    public String required(String dottedPath) {
        JsonNode node = find(dottedPath);
        if (node == null || !node.isValueNode() || node.asText().isBlank()) {
            throw new IllegalStateException("Missing learning-stage configuration: " + dottedPath);
        }
        return node.asText().strip();
    }

    public String value(String dottedPath, String fallback) {
        JsonNode node = find(dottedPath);
        return node == null || !node.isValueNode() || node.asText().isBlank()
                ? fallback
                : node.asText().strip();
    }

    private JsonNode find(String dottedPath) {
        JsonNode node = root;
        for (String name : dottedPath.split("\\.")) {
            node = node == null ? null : node.get(name);
        }
        return node;
    }
}
