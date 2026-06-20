package com.xiaoding.javaai.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiStreamingApplicationContextTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }

    @Test
    void servesFrontendDemoPage() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body)
                .contains("流式输出")
                .contains("/api/stream/ticket-advice")
                        .contains("lastEventIdInput")
                        .contains("lastEventIdValue")
                        .contains("answer.classList.add('cursor')")
                        .contains("逐字显示"));
    }
}
