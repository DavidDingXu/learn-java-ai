package com.xiaoding.javaai.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationValidatorTest {

    private static final Map<String, String> VALID_PRODUCTION = Map.ofEntries(
            Map.entry("java-ai.persistence.mode", "jdbc"),
            Map.entry("spring.flyway.enabled", "true"),
            Map.entry("spring.datasource.url",
                    "jdbc:postgresql://postgres.company.test:5432/ticket"),
            Map.entry("spring.datasource.username", "ticket"),
            Map.entry("spring.datasource.password", "database-password"),
            Map.entry("spring.ai.model.chat", "openai"),
            Map.entry("spring.ai.openai.api-key", "test-key-not-a-real-credential"),
            Map.entry("spring.ai.openai.base-url", "https://ai.company.test/v1"),
            Map.entry("spring.ai.openai.chat.model", "chat-model"),
            Map.entry("java-ai.security.jwt.enabled", "true"),
            Map.entry("java-ai.security.jwt.issuer", "https://identity.company.test"),
            Map.entry("java-ai.security.jwt.audience", "ticket-agent-service"),
            Map.entry("java-ai.security.jwt.jwk-set-uri",
                    "https://identity.company.test/.well-known/jwks.json"),
            Map.entry("java-ai.agent.downstream-enabled", "false")
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
                "spring.ai.openai.api-key", "replace-with-secret-manager"));

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.ai.openai.api-key")
                .hasMessageNotContaining("replace-with-secret-manager");
    }

    @Test
    void rejectsRemoteToolsWithoutAProductionTokenProvider() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "java-ai.agent.downstream-enabled", "true"));

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("java-ai.agent.downstream-enabled");
    }

    @Test
    void rejectsAnHttpUrlWithoutAHost() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "spring.ai.openai.base-url", "https:model-api"));

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.ai.openai.base-url");
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
