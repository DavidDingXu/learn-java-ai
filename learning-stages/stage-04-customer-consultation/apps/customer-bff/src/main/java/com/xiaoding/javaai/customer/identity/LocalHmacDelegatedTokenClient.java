package com.xiaoding.javaai.customer.identity;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class LocalHmacDelegatedTokenClient implements DelegatedTokenClient {

    private final NimbusJwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final String actor;
    private final String scope;
    private final Duration lifetime;

    public LocalHmacDelegatedTokenClient(
            String hmacSecret,
            String issuer,
            String audience,
            String actor,
            Duration lifetime
    ) {
        this(hmacSecret, issuer, audience, actor, "knowledge:answer", lifetime);
    }

    public LocalHmacDelegatedTokenClient(
            String hmacSecret,
            String issuer,
            String audience,
            String actor,
            String scope,
            Duration lifetime
    ) {
        byte[] secretBytes = hmacSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("Development JWT HMAC secret must contain at least 32 bytes");
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secretBytes, "HmacSHA256")));
        this.issuer = requireText(issuer, "issuer");
        this.audience = requireText(audience, "audience");
        this.actor = requireText(actor, "actor");
        this.scope = requireText(scope, "scope");
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive");
        }
        this.lifetime = lifetime;
    }

    @Override
    public Mono<DelegatedAccessToken> exchange(CustomerAccessToken source) {
        return Mono.fromSupplier(() -> sign(source.identity()));
    }

    private DelegatedAccessToken sign(CustomerIdentity identity) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(identity.subject())
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("scope", scope)
                .claim("tenantId", identity.tenantId())
                .claim("roles", identity.roles())
                .claim("departmentIds", identity.departments())
                .claim("act", Map.of("sub", actor))
                .build();
        String tokenValue = encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new DelegatedAccessToken(tokenValue, expiresAt);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
