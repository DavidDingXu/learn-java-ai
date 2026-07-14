package com.xiaoding.javaai.ticket.security;

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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "java-ai.security.jwt.enabled", havingValue = "true")
public class DelegatedJwtConfiguration {

    @Bean
    JwtDecoder ticketDelegatedJwtDecoder(Environment environment) {
        String issuer = required(environment, "java-ai.security.jwt.issuer");
        String audience = required(environment, "java-ai.security.jwt.audience");
        NimbusJwtDecoder decoder = createDecoder(environment);
        OAuth2TokenValidator<Jwt> standard = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = validator(
                "invalid_audience", "Delegated token audience must contain " + audience,
                jwt -> jwt.getAudience().contains(audience));
        OAuth2TokenValidator<Jwt> tenantValidator = validator(
                "missing_tenant", "Delegated token must contain tenantId",
                jwt -> StringUtils.hasText(jwt.getClaimAsString("tenantId")));
        OAuth2TokenValidator<Jwt> subjectValidator = validator(
                "missing_subject", "Delegated token must contain sub",
                jwt -> StringUtils.hasText(jwt.getSubject()));
        OAuth2TokenValidator<Jwt> actorValidator = validator(
                "missing_actor", "Delegated token must contain act.sub",
                jwt -> StringUtils.hasText(actorSubject(jwt)));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                standard, audienceValidator, tenantValidator, subjectValidator, actorValidator));
        return decoder;
    }

    private static NimbusJwtDecoder createDecoder(Environment environment) {
        String secret = environment.getProperty("java-ai.security.jwt.hmac-secret", "");
        if (StringUtils.hasText(secret)) {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) throw new IllegalStateException("Development JWT HMAC secret must contain at least 32 bytes");
            return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }
        String jwkSetUri = environment.getProperty("java-ai.security.jwt.jwk-set-uri", "");
        if (StringUtils.hasText(jwkSetUri)) return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        throw new IllegalStateException("Ticket JWT security requires jwk-set-uri or a development hmac-secret");
    }

    private static String actorSubject(Jwt jwt) {
        Map<String, Object> actor = jwt.getClaimAsMap("act");
        return actor == null ? null : String.valueOf(actor.get("sub"));
    }

    private static OAuth2TokenValidator<Jwt> validator(
            String code,
            String description,
            java.util.function.Predicate<Jwt> predicate
    ) {
        OAuth2Error error = new OAuth2Error(code, description, null);
        return jwt -> predicate.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(error);
    }

    private static String required(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (!StringUtils.hasText(value)) throw new IllegalStateException("Missing required property: " + property);
        return value;
    }
}
