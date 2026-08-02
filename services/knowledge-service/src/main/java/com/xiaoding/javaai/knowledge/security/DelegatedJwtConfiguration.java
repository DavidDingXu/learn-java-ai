package com.xiaoding.javaai.knowledge.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.util.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(name = "java-ai.security.mode", havingValue = "jwt")
public class DelegatedJwtConfiguration {

    @Bean
    ReactiveJwtDecoder delegatedJwtDecoder(Environment environment) {
        String issuer = required(environment, "java-ai.security.jwt.issuer");
        String audience = required(environment, "java-ai.security.jwt.audience");
        Set<String> allowedActors = allowedActors(environment);
        NimbusReactiveJwtDecoder decoder = createDecoder(environment);

        OAuth2TokenValidator<Jwt> standard = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = requiredClaim(
                "invalid_audience",
                "Delegated token audience must contain " + audience,
                jwt -> jwt.getAudience().contains(audience)
        );
        OAuth2TokenValidator<Jwt> tenantValidator = requiredClaim(
                "missing_tenant",
                "Delegated token must contain tenantId",
                jwt -> StringUtils.hasText(jwt.getClaimAsString("tenantId"))
        );
        OAuth2TokenValidator<Jwt> subjectValidator = requiredClaim(
                "missing_subject",
                "Delegated token must contain sub",
                jwt -> StringUtils.hasText(jwt.getSubject())
        );
        OAuth2TokenValidator<Jwt> actorValidator = requiredClaim(
                "invalid_actor",
                "Delegated token actor is not allowed",
                jwt -> StringUtils.hasText(actorSubject(jwt))
                        && allowedActors.contains(actorSubject(jwt))
        );
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                standard, audienceValidator, subjectValidator, tenantValidator, actorValidator));
        return decoder;
    }

    private static NimbusReactiveJwtDecoder createDecoder(Environment environment) {
        String hmacSecret = environment.getProperty("java-ai.security.jwt.hmac-secret", "");
        String jwkSetUri = environment.getProperty("java-ai.security.jwt.jwk-set-uri", "");
        if (StringUtils.hasText(hmacSecret) && StringUtils.hasText(jwkSetUri)) {
            throw new IllegalStateException(
                    "Configure exactly one JWT verification source: jwk-set-uri or hmac-secret");
        }
        if (StringUtils.hasText(hmacSecret)) {
            byte[] secretBytes = hmacSecret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                throw new IllegalStateException("Development JWT HMAC secret must contain at least 32 bytes");
            }
            SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
            return NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }

        if (StringUtils.hasText(jwkSetUri)) {
            return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        throw new IllegalStateException(
                "JWT security requires java-ai.security.jwt.jwk-set-uri; local development may use hmac-secret");
    }

    private static OAuth2TokenValidator<Jwt> requiredClaim(
            String errorCode,
            String description,
            java.util.function.Predicate<Jwt> predicate
    ) {
        OAuth2Error error = new OAuth2Error(errorCode, description, null);
        return jwt -> predicate.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(error);
    }

    private static String actorSubject(Jwt jwt) {
        Map<String, Object> actor = jwt.getClaimAsMap("act");
        return actor == null ? null : String.valueOf(actor.get("sub"));
    }

    private static Set<String> allowedActors(Environment environment) {
        String configured = required(environment, "java-ai.security.jwt.allowed-actors");
        Set<String> actors = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
        if (actors.isEmpty()) {
            throw new IllegalStateException("At least one delegated JWT actor must be allowed");
        }
        return actors;
    }

    private static String required(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required property: " + property);
        }
        return value;
    }
}
