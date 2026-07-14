package com.xiaoding.javaai.customer.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.Duration;

public final class OAuth2TokenExchangeDelegatedTokenClient implements DelegatedTokenClient {

    private static final String TOKEN_EXCHANGE_GRANT =
            "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String ACCESS_TOKEN_TYPE =
            "urn:ietf:params:oauth:token-type:access_token";

    private final WebClient webClient;
    private final String tokenEndpoint;
    private final String audience;
    private final String scope;
    private final String clientId;
    private final String clientSecret;
    private final Duration timeout;

    public OAuth2TokenExchangeDelegatedTokenClient(
            WebClient.Builder webClientBuilder,
            String tokenEndpoint,
            String audience,
            String scope
    ) {
        this(webClientBuilder, tokenEndpoint, audience, scope,
                null, null, Duration.ofSeconds(5));
    }

    public OAuth2TokenExchangeDelegatedTokenClient(
            WebClient.Builder webClientBuilder,
            String tokenEndpoint,
            String audience,
            String scope,
            String clientId,
            String clientSecret,
            Duration timeout
    ) {
        this.webClient = webClientBuilder.build();
        this.tokenEndpoint = requireText(tokenEndpoint, "tokenEndpoint");
        this.audience = requireText(audience, "audience");
        this.scope = requireText(scope, "scope");
        if ((clientId == null) != (clientSecret == null)) {
            throw new IllegalArgumentException("clientId and clientSecret must be configured together");
        }
        this.clientId = normalize(clientId);
        this.clientSecret = normalize(clientSecret);
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
    }

    @Override
    public Mono<DelegatedAccessToken> exchange(CustomerAccessToken source) {
        WebClient.RequestBodySpec request = webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (clientId != null) {
            request.headers(headers -> headers.setBasicAuth(clientId, clientSecret));
        }
        return request
                .body(BodyInserters.fromFormData("grant_type", TOKEN_EXCHANGE_GRANT)
                        .with("subject_token", source.tokenValue())
                        .with("subject_token_type", ACCESS_TOKEN_TYPE)
                        .with("requested_token_type", ACCESS_TOKEN_TYPE)
                        .with("audience", audience)
                        .with("scope", scope))
                .retrieve()
                .bodyToMono(TokenExchangeResponse.class)
                .timeout(timeout)
                .map(response -> new DelegatedAccessToken(
                        response.accessToken(),
                        Instant.now().plusSeconds(response.expiresIn())
                ));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record TokenExchangeResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
        private TokenExchangeResponse {
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("Token Exchange response is missing access_token");
            }
            if (expiresIn <= 0) {
                throw new IllegalArgumentException("Token Exchange response has invalid expires_in");
            }
        }
    }
}
