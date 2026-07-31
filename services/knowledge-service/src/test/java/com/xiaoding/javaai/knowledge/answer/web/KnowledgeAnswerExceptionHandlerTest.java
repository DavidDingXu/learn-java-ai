package com.xiaoding.javaai.knowledge.answer.web;

import com.xiaoding.javaai.knowledge.answer.application.ModelProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeAnswerExceptionHandlerTest {

    private final WebTestClient client = WebTestClient.bindToController(new FailureController())
            .controllerAdvice(new KnowledgeAnswerExceptionHandler())
            .build();

    @Test
    void mapsProviderRateLimitsWithoutExposingTheProviderMessage() {
        assertStableError(
                ModelProviderException.Reason.RATE_LIMITED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "MODEL_RATE_LIMITED"
        );
    }

    @Test
    void mapsExhaustedProviderFailuresWithoutExposingTheProviderMessage() {
        assertStableError(
                ModelProviderException.Reason.UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                "MODEL_PROVIDER_UNAVAILABLE"
        );
    }

    @Test
    void mapsRejectedProviderRequestsWithoutExposingTheProviderMessage() {
        assertStableError(
                ModelProviderException.Reason.REQUEST_REJECTED,
                HttpStatus.BAD_GATEWAY,
                "MODEL_PROVIDER_REQUEST_REJECTED"
        );
    }

    private void assertStableError(
            ModelProviderException.Reason reason,
            HttpStatus status,
            String code
    ) {
        client.get()
                .uri(uriBuilder -> uriBuilder.path("/failure")
                        .queryParam("reason", reason.name())
                        .build())
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectBody()
                .jsonPath("$.code").isEqualTo(code)
                .consumeWith(result -> assertThat(new String(
                        result.getResponseBody(), StandardCharsets.UTF_8
                )).doesNotContain("secret-provider-message"));
    }

    @RestController
    private static final class FailureController {

        @GetMapping("/failure")
        Mono<Void> fail(@RequestParam("reason") ModelProviderException.Reason reason) {
            return Mono.error(new ModelProviderException(
                    reason,
                    new IllegalStateException("secret-provider-message")
            ));
        }
    }
}
