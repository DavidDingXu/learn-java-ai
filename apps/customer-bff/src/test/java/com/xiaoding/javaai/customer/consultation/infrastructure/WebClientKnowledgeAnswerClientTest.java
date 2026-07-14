package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerClient;
import com.xiaoding.javaai.customer.consultation.domain.ConversationContextView;
import com.xiaoding.javaai.customer.consultation.domain.ConversationRole;
import com.xiaoding.javaai.customer.consultation.domain.ConversationTurn;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientKnowledgeAnswerClientTest {

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
    void sends_delegated_identity_and_untrusted_conversation_context() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "answer":"退款通常在 1 到 5 个工作日到账。",
                          "citations":[{"documentId":"refund-policy","version":"v1","sectionId":"arrival-time","title":"退款到账时间"}],
                          "refused":false,
                          "refusalReason":null,
                          "model":"fixture-model",
                          "usage":{"promptTokens":10,"completionTokens":8,"totalTokens":18},
                          "finishReason":"stop",
                          "traceId":"trace-123"
                        }
                        """));
        WebClientKnowledgeAnswerClient client = new WebClientKnowledgeAnswerClient(
                WebClient.builder(), server.url("/").toString(), Duration.ofSeconds(2));

        var result = client.answer(
                new DelegatedAccessToken("delegated-token", Instant.now().plusSeconds(60)),
                new KnowledgeAnswerClient.Request(
                        "那银行卡会更慢吗？",
                        new ConversationContextView(
                                "此前讨论过退款；结果：已回答",
                                List.of(new ConversationTurn(
                                        ConversationRole.USER,
                                        "退款多久到账？",
                                        "attempt-1",
                                        Instant.parse("2026-07-13T04:00:00Z")
                                ))
                        )
                )
        ).block();

        var request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/knowledge/answers");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer delegated-token");
        assertThat(request.getBody().readUtf8())
                .contains("\"question\":\"那银行卡会更慢吗？\"")
                .contains("\"conversationContext\"")
                .contains("\"role\":\"USER\"")
                .doesNotContain("tenant-a");
        assertThat(result.traceId()).isEqualTo("trace-123");
        assertThat(result.citations()).singleElement()
                .extracting(citation -> citation.sectionId())
                .isEqualTo("arrival-time");
    }
}
