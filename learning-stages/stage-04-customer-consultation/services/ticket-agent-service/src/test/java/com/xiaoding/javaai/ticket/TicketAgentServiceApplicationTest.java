package com.xiaoding.javaai.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketAgentServiceApplicationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private RestTestClient client;

    @Test
    void testConfigurationStartsWithoutExternalInfrastructureAndExposesOnlyHealth() {
        assertThat(environment.matchesProfiles("test")).isTrue();
        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo("ticket-agent-service");

        client.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");

        client.get()
                .uri("/actuator/env")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
