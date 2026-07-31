package com.xiaoding.javaai.customer.consultation.web;

import com.xiaoding.javaai.customer.downstream.DownstreamServiceException;
import com.xiaoding.javaai.customer.downstream.DownstreamTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerConsultationExceptionHandlerTest {

    private final WebTestClient client = WebTestClient.bindToController(new FailureController())
            .controllerAdvice(new CustomerConsultationExceptionHandler())
            .build();

    @Test
    void mapsDownstreamTimeoutsWithoutExposingTheDependency() {
        client.get()
                .uri("/timeout")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DOWNSTREAM_TIMEOUT")
                .jsonPath("$.message").isEqualTo("A downstream service did not respond in time")
                .consumeWith(result -> assertThat(new String(
                        result.getResponseBody(), StandardCharsets.UTF_8
                )).doesNotContain("knowledge-service"));
    }

    @Test
    void mapsDownstreamFailuresWithoutExposingTheDependency() {
        client.get()
                .uri("/failure")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DOWNSTREAM_SERVICE_FAILED")
                .jsonPath("$.message").isEqualTo("A downstream service request failed")
                .consumeWith(result -> assertThat(new String(
                        result.getResponseBody(), StandardCharsets.UTF_8
                )).doesNotContain("knowledge-service").doesNotContain("503"));
    }

    @RestController
    private static final class FailureController {

        @GetMapping("/timeout")
        Mono<Void> timeout() {
            return Mono.error(new DownstreamTimeoutException(
                    "knowledge-service",
                    new TimeoutException("internal timeout details")
            ));
        }

        @GetMapping("/failure")
        Mono<Void> fail() {
            return Mono.error(new DownstreamServiceException("knowledge-service", 503));
        }
    }
}
