package com.xiaoding.javaai.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StreamingControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void streamsTicketAdviceWithExplicitSessionIdQueryParameter() {
        byte[] body = webTestClient.get()
                .uri("/api/stream/ticket-advice?sessionId=s-test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
                .expectBody()
                .returnResult()
                .getResponseBody();

        String eventStream = new String(body, StandardCharsets.UTF_8);
        assertThat(eventStream).contains("id:s-test-1");
        assertThat(eventStream).contains("event:token");
        assertThat(eventStream).contains("data:先核对订单");
        assertThat(eventStream).contains("id:s-test-3");
        assertThat(eventStream).contains("event:done");
    }

    @Test
    void resumesAfterEventIdPassedAsQueryParameter() {
        byte[] body = webTestClient.get()
                .uri("/api/stream/ticket-advice?sessionId=s1001&lastEventId=s1001-1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
                .expectBody()
                .returnResult()
                .getResponseBody();

        String eventStream = new String(body, StandardCharsets.UTF_8);
        assertThat(eventStream).doesNotContain("id:s1001-1");
        assertThat(eventStream).contains("id:s1001-2");
        assertThat(eventStream).contains("data:再检索退款制度");
        assertThat(eventStream).contains("id:s1001-3");
    }

    @Test
    void resumesAfterLastEventIdRequestHeaderForBrowserReconnect() {
        byte[] body = webTestClient.get()
                .uri("/api/stream/ticket-advice?sessionId=s1001")
                .header("Last-Event-ID", "s1001-1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
                .expectBody()
                .returnResult()
                .getResponseBody();

        String eventStream = new String(body, StandardCharsets.UTF_8);
        assertThat(eventStream).doesNotContain("id:s1001-1");
        assertThat(eventStream).contains("id:s1001-2");
        assertThat(eventStream).contains("data:再检索退款制度");
    }

    @Test
    void treatsSessionIdInputWithNumericSuffixAsResumeCursorForFrontendDemo() {
        byte[] body = webTestClient.get()
                .uri("/api/stream/ticket-advice?sessionId=s1001-2")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
                .expectBody()
                .returnResult()
                .getResponseBody();

        String eventStream = new String(body, StandardCharsets.UTF_8);
        assertThat(eventStream).doesNotContain("id:s1001-1");
        assertThat(eventStream).doesNotContain("id:s1001-2");
        assertThat(eventStream).contains("id:s1001-3");
        assertThat(eventStream).contains("event:done");
    }
}
