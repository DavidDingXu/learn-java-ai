package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.xiaoding.javaai.ticket.agent.application.DownstreamAccessTokenProvider;
import com.xiaoding.javaai.ticket.task.AgentTask;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

final class DevelopmentHmacDownstreamAccessTokenProvider implements DownstreamAccessTokenProvider {

    private final String issuer;
    private final String actorId;
    private final Duration timeToLive;
    private final Clock clock;
    private final Supplier<String> tokenIdGenerator;
    private final NimbusJwtEncoder encoder;

    DevelopmentHmacDownstreamAccessTokenProvider(
            String issuer,
            String hmacSecret,
            String actorId,
            Duration timeToLive,
            Clock clock,
            Supplier<String> tokenIdGenerator
    ) {
        this.issuer = requireText(issuer, "issuer");
        this.actorId = requireText(actorId, "actorId");
        this.timeToLive = requirePositive(timeToLive, "timeToLive");
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
        this.tokenIdGenerator = java.util.Objects.requireNonNull(
                tokenIdGenerator, "tokenIdGenerator must not be null");
        byte[] secret = requireText(hmacSecret, "hmacSecret").getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalArgumentException("development hmacSecret must contain at least 32 bytes");
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret, "HmacSHA256")));
    }

    @Override
    public String tokenFor(AgentTask task, String audience, String scope) {
        java.util.Objects.requireNonNull(task, "task must not be null");
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(task.identity().subjectId())
                .audience(java.util.List.of(requireText(audience, "audience")))
                .issuedAt(now)
                .expiresAt(now.plus(timeToLive))
                .id(requireText(tokenIdGenerator.get(), "tokenId"))
                .claim("tenantId", task.identity().tenantId())
                .claim("act", Map.of("sub", actorId))
                .claim("scope", requireText(scope, "scope"))
                .claim("roles", task.identity().roles())
                .claim("departmentIds", task.identity().departments())
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
