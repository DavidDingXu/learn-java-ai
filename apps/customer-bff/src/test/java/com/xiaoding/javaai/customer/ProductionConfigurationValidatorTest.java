package com.xiaoding.javaai.customer;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationValidatorTest {

    private static final Map<String, String> VALID_PRODUCTION = Map.ofEntries(
            Map.entry("java-ai.runtime.external-integrations-enabled", "true"),
            Map.entry("java-ai.security.customer-jwt.enabled", "true"),
            Map.entry("java-ai.security.customer-jwt.issuer", "https://identity.company.test"),
            Map.entry("java-ai.security.customer-jwt.audience", "customer-bff"),
            Map.entry("java-ai.security.customer-jwt.jwk-set-uri",
                    "https://identity.company.test/.well-known/jwks.json"),
            Map.entry("java-ai.identity.token-endpoint",
                    "https://identity.company.test/oauth2/token"),
            Map.entry("java-ai.identity.client-id", "customer-bff"),
            Map.entry("java-ai.identity.client-secret", "client-secret"),
            Map.entry("java-ai.downstream.knowledge.base-url", "http://knowledge-service:8081"),
            Map.entry("java-ai.downstream.ticket.base-url", "http://ticket-agent-service:8082")
    );

    @Test
    void ignoresNonProductionProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("demo");

        assertThatCode(() -> ProductionConfigurationValidator.validate(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsRepositoryPlaceholdersBeforeCreatingHttpClients() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "java-ai.identity.token-endpoint",
                "https://identity.example.com/oauth2/token"));

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("java-ai.identity.token-endpoint")
                .hasMessageNotContaining("identity.example.com");
    }

    @Test
    void rejectsTheExampleRootDomain() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "java-ai.identity.token-endpoint", "https://example.com/oauth2/token"));

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("java-ai.identity.token-endpoint")
                .hasMessageNotContaining("https://example.com");
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
