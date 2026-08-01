package com.xiaoding.javaai.ticket;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class ProductionConfigurationValidator {

    private ProductionConfigurationValidator() {
    }

    static void validate(Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("production"))) {
            return;
        }

        requireEquals(environment, "java-ai.persistence.mode", "jdbc");
        requireTrue(environment, "spring.flyway.enabled");
        requireJdbcUrl(environment, "spring.datasource.url");
        requireValue(environment, "spring.datasource.username");
        requireValue(environment, "spring.datasource.password");

        requireEquals(environment, "spring.ai.model.chat", "openai");
        requireValue(environment, "spring.ai.openai.api-key");
        requireHttpUrl(environment, "spring.ai.openai.base-url");
        requireValue(environment, "spring.ai.openai.chat.model");

        requireTrue(environment, "java-ai.security.jwt.enabled");
        requireHttpUrl(environment, "java-ai.security.jwt.issuer");
        requireValue(environment, "java-ai.security.jwt.audience");
        requireJwtVerificationSource(environment, "java-ai.security.jwt");

        if (Boolean.TRUE.equals(environment.getProperty(
                "java-ai.agent.downstream-enabled", Boolean.class))) {
            throw invalid(
                    "java-ai.agent.downstream-enabled",
                    "must remain false until a production token provider is implemented");
        }
    }

    private static void requireJwtVerificationSource(Environment environment, String prefix) {
        String jwkSetUri = optional(environment, prefix + ".jwk-set-uri");
        String hmacSecret = optional(environment, prefix + ".hmac-secret");
        if (StringUtils.hasText(jwkSetUri) == StringUtils.hasText(hmacSecret)) {
            throw invalid(prefix, "configure exactly one of jwk-set-uri or hmac-secret");
        }
        if (StringUtils.hasText(jwkSetUri)) {
            requireHttpUrl(environment, prefix + ".jwk-set-uri");
        } else {
            String validatedSecret = requireValue(environment, prefix + ".hmac-secret");
            if (validatedSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw invalid(prefix + ".hmac-secret", "must contain at least 32 bytes");
            }
        }
    }

    private static void requireTrue(Environment environment, String key) {
        if (!Boolean.TRUE.equals(environment.getProperty(key, Boolean.class))) {
            throw invalid(key, "must be true");
        }
    }

    private static void requireEquals(Environment environment, String key, String expected) {
        if (!expected.equals(environment.getProperty(key))) {
            throw invalid(key, "must be " + expected);
        }
    }

    private static void requireJdbcUrl(Environment environment, String key) {
        String value = requireValue(environment, key);
        if (!value.startsWith("jdbc:postgresql://")) {
            throw invalid(key, "must be a PostgreSQL JDBC URL");
        }
    }

    private static void requireHttpUrl(Environment environment, String key) {
        String value = requireValue(environment, key);
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalid(key, "must be an absolute HTTP(S) URL");
        }
        if (!uri.isAbsolute()
                || !StringUtils.hasText(uri.getHost())
                || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw invalid(key, "must be an absolute HTTP(S) URL");
        }
    }

    private static String requireValue(Environment environment, String key) {
        String value = optional(environment, key);
        if (!StringUtils.hasText(value) || containsPlaceholder(value)) {
            throw invalid(key, "must be set to a non-placeholder value");
        }
        return value;
    }

    private static String optional(Environment environment, String key) {
        return environment.getProperty(key, "").trim();
    }

    private static boolean containsPlaceholder(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("replace-with")
                || normalized.contains("://example.com")
                || normalized.contains(".example.com");
    }

    private static IllegalStateException invalid(String key, String requirement) {
        return new IllegalStateException(
                "Invalid production configuration: " + key + " " + requirement);
    }
}
