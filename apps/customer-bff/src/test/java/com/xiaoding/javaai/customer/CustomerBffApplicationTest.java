package com.xiaoding.javaai.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerBffApplicationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testConfigurationStartsWithoutExternalInfrastructureAndExposesOnlyHealth() {
        assertThat(environment.matchesProfiles("test")).isTrue();
        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo("customer-bff");
        assertThat(environment.getProperty(
                "java-ai.runtime.external-integrations-enabled", Boolean.class))
                .isFalse();

        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");

        webTestClient.get()
                .uri("/actuator/env")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
