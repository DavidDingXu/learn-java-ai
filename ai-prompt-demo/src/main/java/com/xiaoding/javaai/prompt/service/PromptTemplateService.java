package com.xiaoding.javaai.prompt.service;

import org.springframework.ai.template.ValidationMode;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PromptTemplateService {

    private final Map<String, Map<String, PromptTemplate>> templates = new ConcurrentHashMap<>();
    private final StTemplateRenderer renderer = StTemplateRenderer.builder()
            .startDelimiterToken('{')
            .endDelimiterToken('}')
            .validationMode(ValidationMode.THROW)
            .build();

    public PromptTemplate save(String code, String version, String content) {
        PromptTemplate template = new PromptTemplate(code, version, content, Instant.now());
        templates.computeIfAbsent(code, ignored -> new LinkedHashMap<>()).put(version, template);
        return template;
    }

    public PromptTemplate findLatest(String code) {
        Map<String, PromptTemplate> versions = templates.get(code);
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("prompt template not found: " + code);
        }
        return versions.values().stream()
                .max(Comparator.comparing(PromptTemplate::createdAt))
                .orElseThrow();
    }

    public PromptTemplate findVersion(String code, String version) {
        Map<String, PromptTemplate> versions = templates.get(code);
        if (versions == null || !versions.containsKey(version)) {
            throw new IllegalArgumentException("prompt template version not found: " + code + ":" + version);
        }
        return versions.get(version);
    }

    public PromptTemplate rollback(String code, String version) {
        PromptTemplate source = findVersion(code, version);
        return save(code, "rollback-" + version, source.content());
    }

    public String render(String code, Map<String, String> variables) {
        PromptTemplate template = findLatest(code);
        try {
            return renderer.apply(template.content(), toObjectMap(variables));
        } catch (IllegalStateException error) {
            throw new IllegalArgumentException("unresolved prompt variables for " + code + ": " + error.getMessage(), error);
        }
    }

    private Map<String, Object> toObjectMap(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return Map.of();
        }
        return variables.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
