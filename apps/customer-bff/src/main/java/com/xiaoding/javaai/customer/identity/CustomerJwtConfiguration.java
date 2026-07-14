package com.xiaoding.javaai.customer.identity;

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

@Configuration
@ConditionalOnProperty(name = "java-ai.security.customer-jwt.enabled", havingValue = "true")
public class CustomerJwtConfiguration {

    @Bean
    ReactiveJwtDecoder customerJwtDecoder(Environment environment) {
        String issuer = required(environment, "java-ai.security.customer-jwt.issuer");
        String audience = required(environment, "java-ai.security.customer-jwt.audience");
        NimbusReactiveJwtDecoder decoder = createDecoder(environment);
        OAuth2TokenValidator<Jwt> standard = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = validator(
                "invalid_audience", "Customer token audience must contain " + audience,
                jwt -> jwt.getAudience().contains(audience));
        OAuth2TokenValidator<Jwt> tenantValidator = validator(
                "missing_tenant", "Customer token must contain tenantId",
                jwt -> StringUtils.hasText(jwt.getClaimAsString("tenantId")));
        OAuth2TokenValidator<Jwt> subjectValidator = validator(
                "missing_subject", "Customer token must contain sub",
                jwt -> StringUtils.hasText(jwt.getSubject()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                standard, audienceValidator, tenantValidator, subjectValidator));
        return decoder;
    }

    private static NimbusReactiveJwtDecoder createDecoder(Environment environment) {
        String secret = environment.getProperty("java-ai.security.customer-jwt.hmac-secret", "");
        if (StringUtils.hasText(secret)) {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("Development JWT HMAC secret must contain at least 32 bytes");
            }
            return NimbusReactiveJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }
        String jwkSetUri = environment.getProperty("java-ai.security.customer-jwt.jwk-set-uri", "");
        if (StringUtils.hasText(jwkSetUri)) {
            return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        throw new IllegalStateException("Customer JWT security requires jwk-set-uri or a development hmac-secret");
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
