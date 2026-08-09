package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.PolicyContext;
import com.xiaoding.javaai.knowledge.answer.application.PolicyContextQuery;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

final class ClasspathPolicyContextSource implements PolicyContextSource {

    private final PolicyContext context;

    ClasspathPolicyContextSource(Resource metadataResource, Resource contentResource) {
        this.context = loadContext(metadataResource, contentResource);
    }

    @Override
    public Mono<List<PolicyContext>> load(PolicyContextQuery query) {
        return Mono.just(List.of(context));
    }

    private static PolicyContext loadContext(Resource metadataResource, Resource contentResource) {
        try (InputStreamReader reader = new InputStreamReader(
                metadataResource.getInputStream(), StandardCharsets.UTF_8)) {
            Properties metadata = new Properties();
            metadata.load(reader);
            String content = contentResource.getContentAsString(StandardCharsets.UTF_8);
            return new PolicyContext(
                    required(metadata, "documentId"),
                    required(metadata, "version"),
                    required(metadata, "sectionId"),
                    required(metadata, "title"),
                    content.strip()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load the bundled refund policy context", exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing policy metadata: " + key);
        }
        return value.trim();
    }
}
