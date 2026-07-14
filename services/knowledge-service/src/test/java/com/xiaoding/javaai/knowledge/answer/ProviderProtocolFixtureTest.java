package com.xiaoding.javaai.knowledge.answer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestionCommand;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProviderProtocolFixtureTest {

    private static final MockWebServer PROVIDER = new MockWebServer();

    @BeforeAll
    static void startProvider() throws IOException {
        PROVIDER.start();
    }

    @AfterAll
    static void stopProvider() throws IOException {
        PROVIDER.shutdown();
    }

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.model.chat", () -> "openai");
        registry.add("spring.ai.openai.api-key", () -> "fixture-key");
        registry.add("spring.ai.openai.base-url", () -> PROVIDER.url("/").toString());
        registry.add("spring.ai.openai.chat.model", () -> "fixture-model");
        registry.add("java-ai.runtime.external-integrations-enabled", () -> true);
    }

    @Autowired
    private AnswerKnowledgeQuestion answerKnowledgeQuestion;

    @Test
    void mapsAnOpenAiCompatibleResponseWithoutCallingARealModel() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String structuredAnswer = objectMapper.writeValueAsString(Map.of(
                "answer", "退款审核通过后，通常还需要 1 到 5 个工作日原路到账。",
                "citedSectionIds", List.of("arrival-time"),
                "refused", false,
                "refusalReason", ""
        ));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "id", "chatcmpl-fixture",
                "object", "chat.completion",
                "created", 1750000000,
                "model", "fixture-model",
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", structuredAnswer),
                        "finish_reason", "stop"
                )),
                "usage", Map.of(
                        "prompt_tokens", 52,
                        "completion_tokens", 18,
                        "total_tokens", 70
                )
        ));
        PROVIDER.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(providerResponse));

        StepVerifier.create(answerKnowledgeQuestion.answer(
                        command("退款审核通过了，为什么还没到账？")))
                .assertNext(answer -> {
                    assertThat(answer.answer()).contains("1 到 5 个工作日");
                    assertThat(answer.model()).isEqualTo("fixture-model");
                    assertThat(answer.usage().promptTokens()).isEqualTo(52);
                    assertThat(answer.usage().completionTokens()).isEqualTo(18);
                    assertThat(answer.usage().totalTokens()).isEqualTo(70);
                    assertThat(answer.finishReason()).isEqualTo("stop");
                    assertThat(answer.citations()).singleElement().satisfies(citation -> {
                        assertThat(citation.documentId()).isEqualTo("refund-policy");
                        assertThat(citation.version()).isEqualTo("v1");
                        assertThat(citation.sectionId()).isEqualTo("arrival-time");
                        assertThat(citation.title()).isEqualTo("退款到账时间");
                    });
                })
                .verifyComplete();

        RecordedRequest request = PROVIDER.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/chat/completions");
        assertThat(request.getBody().readUtf8())
                .contains("退款审核通过了，为什么还没到账？")
                .contains("UNTRUSTED_USER_INPUT")
                .contains("TRUSTED_POLICY_CONTEXT")
                .contains("knowledge-answer-v1")
                .contains("refund-policy")
                .contains("1 到 5 个工作日");
    }

    private static AnswerKnowledgeQuestionCommand command(String question) {
        return new AnswerKnowledgeQuestionCommand(
                question,
                new KnowledgeAccessScope(new TenantId("tenant-test"), "user-test", List.of()),
                Instant.parse("2026-07-13T03:00:00Z")
        );
    }
}
