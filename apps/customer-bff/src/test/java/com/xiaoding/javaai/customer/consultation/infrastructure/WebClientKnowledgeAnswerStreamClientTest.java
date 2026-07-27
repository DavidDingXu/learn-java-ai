package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerClient;
import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerStreamClient;
import com.xiaoding.javaai.customer.consultation.domain.ConversationContextView;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientKnowledgeAnswerStreamClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void decodes_named_sse_events_and_preserves_cancellation() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        event:metadata
                        data:{"traceId":"trace-123","promptVersion":"v1"}

                        event:delta
                        data:{"text":"退款通常在 1 到 5 个工作日到账。"}

                        event:citation
                        data:{"citation":{"documentId":"refund-policy","version":"v1","sectionId":"arrival-time","title":"退款到账时间"}}

                        event:completed
                        data:{"model":"model-a","usage":{"promptTokens":1,"completionTokens":1,"totalTokens":2},"finishReason":"stop","ttftMillis":20,"refused":false,"refusalReason":null}

                        """));
        WebClientKnowledgeAnswerStreamClient client = new WebClientKnowledgeAnswerStreamClient(
                WebClient.builder(), server.url("/").toString(), Duration.ofSeconds(2));

        StepVerifier.create(client.stream(
                        new DelegatedAccessToken("delegated-token", Instant.now().plusSeconds(60)),
                        new KnowledgeAnswerClient.Request(
                                "退款多久到账？", new ConversationContextView("", List.of()))))
                .assertNext(event -> assertThat(event)
                        .isInstanceOf(KnowledgeAnswerStreamClient.Metadata.class))
                .assertNext(event -> assertThat(event)
                        .isEqualTo(new KnowledgeAnswerStreamClient.Delta(
                                "退款通常在 1 到 5 个工作日到账。")))
                .assertNext(event -> assertThat(event)
                        .isInstanceOf(KnowledgeAnswerStreamClient.Citation.class))
                .assertNext(event -> assertThat(event)
                        .isEqualTo(new KnowledgeAnswerStreamClient.Completed(false, null)))
                .verifyComplete();

        var request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/knowledge/answers/stream");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer delegated-token");
    }

    @Test
    void mapsTheRefusalDecisionFromTheCompletedEvent() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        event:completed
                        data:{"model":"model-a","usage":{"promptTokens":1,"completionTokens":1,"totalTokens":2},"finishReason":"stop","ttftMillis":20,"refused":true,"refusalReason":"缺少退款审核状态"}

                        """));
        WebClientKnowledgeAnswerStreamClient client = new WebClientKnowledgeAnswerStreamClient(
                WebClient.builder(), server.url("/").toString(), Duration.ofSeconds(2));

        StepVerifier.create(client.stream(
                        new DelegatedAccessToken("delegated-token", Instant.now().plusSeconds(60)),
                        new KnowledgeAnswerClient.Request(
                                "退款审核通过了吗？", new ConversationContextView("", List.of()))))
                .expectNext(new KnowledgeAnswerStreamClient.Completed(
                        true, "缺少退款审核状态"))
                .verifyComplete();
    }
}
