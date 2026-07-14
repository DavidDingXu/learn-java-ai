package com.xiaoding.javaai.customer.identity;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2TokenExchangeDelegatedTokenClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void exchangesTheVerifiedCustomerTokenUsingRfc8693() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "access_token": "delegated-token",
                          "issued_token_type": "urn:ietf:params:oauth:token-type:access_token",
                          "token_type": "Bearer",
                          "expires_in": 300,
                          "scope": "knowledge:answer"
                        }
                        """));
        OAuth2TokenExchangeDelegatedTokenClient client = new OAuth2TokenExchangeDelegatedTokenClient(
                WebClient.builder(),
                server.url("/oauth2/token").toString(),
                "knowledge-service",
                "knowledge:answer",
                "customer-bff",
                "client-secret",
                java.time.Duration.ofSeconds(2)
        );
        CustomerIdentity identity = new CustomerIdentity(
                "customer-123",
                "tenant-a",
                Collections.singletonList("CUSTOMER"),
                Collections.singletonList("retail")
        );

        StepVerifier.create(client.exchange(new CustomerAccessToken("customer-token", identity)))
                .assertNext(token -> {
                    assertThat(token.tokenValue()).isEqualTo("delegated-token");
                    assertThat(token.expiresAt()).isAfter(Instant.now());
                })
                .verifyComplete();

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE))
                .startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");
        assertThat(request.getBody().readUtf8())
                .contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange")
                .contains("subject_token=customer-token")
                .contains("subject_token_type=urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Aaccess_token")
                .contains("requested_token_type=urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Aaccess_token")
                .contains("audience=knowledge-service")
                .contains("scope=knowledge%3Aanswer");
    }
}
