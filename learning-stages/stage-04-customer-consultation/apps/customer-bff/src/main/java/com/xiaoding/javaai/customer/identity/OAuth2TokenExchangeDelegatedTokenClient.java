package com.xiaoding.javaai.customer.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiaoding.javaai.customer.downstream.DownstreamServiceException;
import com.xiaoding.javaai.customer.downstream.DownstreamTimeoutException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

public final class OAuth2TokenExchangeDelegatedTokenClient implements DelegatedTokenClient {

    private static final String TOKEN_EXCHANGE_GRANT =
            "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String ACCESS_TOKEN_TYPE =
            "urn:ietf:params:oauth:token-type:access_token";
    private static final String BEARER_TOKEN_TYPE = "Bearer";

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
        this.clientId = normalize(clientId);
        this.clientSecret = normalize(clientSecret);
        if ((this.clientId == null) != (this.clientSecret == null)) {
            throw new IllegalArgumentException("clientId and clientSecret must be configured together");
        }
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
                .onStatus(status -> status.isError(), response -> Mono.error(
                        new DownstreamServiceException(
                                "identity-provider", response.statusCode().value())))
                .bodyToMono(TokenExchangeResponse.class)
                .switchIfEmpty(Mono.error(new DownstreamServiceException(
                        "identity-provider", new IllegalStateException("empty response body"))))
                .map(response -> new DelegatedAccessToken(
                        response.accessToken(),
                        Instant.now().plusSeconds(response.expiresIn())
                ))
                .timeout(timeout)
                .onErrorMap(TimeoutException.class,
                        error -> new DownstreamTimeoutException("identity-provider", error))
                .onErrorMap(WebClientRequestException.class,
                        error -> new DownstreamServiceException("identity-provider", error))
                .onErrorMap(DecodingException.class,
                        error -> new DownstreamServiceException("identity-provider", error))
                .onErrorMap(IllegalArgumentException.class,
                        error -> new DownstreamServiceException("identity-provider", error));
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
            @JsonProperty("issued_token_type") String issuedTokenType,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {
        private TokenExchangeResponse {
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("Token Exchange response is missing access_token");
            }
            if (!ACCESS_TOKEN_TYPE.equals(issuedTokenType)) {
                throw new IllegalArgumentException("Token Exchange response has invalid issued_token_type");
            }
            if (tokenType == null || !BEARER_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
                throw new IllegalArgumentException("Token Exchange response has invalid token_type");
            }
            if (expiresIn <= 0) {
                throw new IllegalArgumentException("Token Exchange response has invalid expires_in");
            }
        }
    }
}
