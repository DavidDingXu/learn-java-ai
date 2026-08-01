package com.xiaoding.javaai.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeServiceApplicationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testConfigurationStartsWithoutExternalInfrastructureAndExposesOnlyHealth() {
        assertThat(environment.matchesProfiles("test")).isTrue();
        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo("knowledge-service");

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

        webTestClient.post()
                .uri("/api/v1/knowledge/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"退款什么时候到账？\"}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist("WWW-Authenticate");
    }
}
