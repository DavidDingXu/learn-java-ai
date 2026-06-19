package cn.dingxu.javaai.prompt.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptTemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");

    private final Map<String, Map<String, PromptTemplate>> templates = new ConcurrentHashMap<>();

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
        String rendered = template.content();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        Set<String> unresolvedVariables = unresolvedVariables(rendered);
        if (!unresolvedVariables.isEmpty()) {
            throw new IllegalArgumentException("unresolved prompt variables: " + unresolvedVariables);
        }
        return rendered;
    }

    private Set<String> unresolvedVariables(String rendered) {
        Matcher matcher = VARIABLE_PATTERN.matcher(rendered);
        Set<String> variables = new LinkedHashSet<>();
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }
}
