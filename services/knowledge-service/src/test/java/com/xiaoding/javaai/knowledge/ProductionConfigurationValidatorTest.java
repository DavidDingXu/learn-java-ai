package com.xiaoding.javaai.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationValidatorTest {

    private static final Map<String, String> VALID_PRODUCTION = Map.ofEntries(
            Map.entry("spring.ai.model.chat", "openai"),
            Map.entry("spring.ai.model.embedding", "openai"),
            Map.entry("spring.ai.openai.api-key", "test-key-not-a-real-credential"),
            Map.entry("spring.ai.openai.base-url", "https://ai.company.test/v1"),
            Map.entry("spring.ai.openai.chat.model", "chat-model"),
            Map.entry("spring.ai.openai.embedding.model", "embedding-model"),
            Map.entry("spring.flyway.enabled", "true"),
            Map.entry("java-ai.knowledge.context-source", "retrieval"),
            Map.entry("java-ai.knowledge.ingestion.enabled", "true"),
            Map.entry("java-ai.knowledge.postgres.jdbc-url",
                    "jdbc:postgresql://postgres.company.test:5432/knowledge"),
            Map.entry("java-ai.knowledge.postgres.username", "knowledge"),
            Map.entry("java-ai.knowledge.postgres.password", "database-password"),
            Map.entry("java-ai.knowledge.object-store.local-root", "./var/knowledge-objects"),
            Map.entry("java-ai.security.jwt.enabled", "true"),
            Map.entry("java-ai.security.jwt.issuer", "https://identity.company.test"),
            Map.entry("java-ai.security.jwt.audience", "knowledge-service"),
            Map.entry("java-ai.security.jwt.jwk-set-uri",
                    "https://identity.company.test/.well-known/jwks.json"),
            Map.entry("java-ai.security.jwt.allowed-actors", "customer-bff,ticket-agent-service")
    );

    @Test
    void ignoresNonProductionProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("demo");

        assertThatCode(() -> ProductionConfigurationValidator.validate(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsRepositoryPlaceholdersBeforeCreatingInfrastructureBeans() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "java-ai.knowledge.postgres.jdbc-url",
                "jdbc:postgresql://database.example.com:5432/java_ai_knowledge"));

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("java-ai.knowledge.postgres.jdbc-url")
                .hasMessageNotContaining("database.example.com");
    }

    @Test
    void rejectsAPlaceholderHmacSecretEvenWhenItIsLongEnough() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "java-ai.security.jwt.jwk-set-uri", "",
                "java-ai.security.jwt.hmac-secret",
                "replace-with-a-secret-manager-value-longer-than-32-bytes"));

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("java-ai.security.jwt.hmac-secret")
                .hasMessageNotContaining("replace-with-a-secret-manager-value");
    }

    @Test
    void acceptsACompleteProductionConfiguration() {
        MockEnvironment environment = productionEnvironment(Map.of());

        assertThatCode(() -> ProductionConfigurationValidator.validate(environment))
                .doesNotThrowAnyException();
    }

    private static MockEnvironment productionEnvironment(Map<String, String> overrides) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        VALID_PRODUCTION.forEach(environment::setProperty);
        overrides.forEach(environment::setProperty);
        return environment;
    }
}
