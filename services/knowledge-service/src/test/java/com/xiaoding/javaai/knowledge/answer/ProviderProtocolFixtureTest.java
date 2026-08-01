package com.xiaoding.javaai.knowledge.answer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestionCommand;
import com.xiaoding.javaai.knowledge.answer.application.InvalidModelAnswerException;
import com.xiaoding.javaai.knowledge.answer.application.ModelProviderException;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerModel;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        registry.add("spring.ai.openai.max-retries", () -> 0);
        registry.add("java-ai.knowledge.answer.total-timeout", () -> "1s");
        registry.add("java-ai.knowledge.answer.retry.delay", () -> "10ms");
    }

    @Autowired
    private AnswerKnowledgeQuestion answerKnowledgeQuestion;

    @Autowired
    private KnowledgeAnswerModel knowledgeAnswerModel;

    @Test
    void appliesResilienceAnnotationsThroughASpringProxy() {
        assertThat(AopUtils.isAopProxy(knowledgeAnswerModel)).isTrue();
        assertThat(AopUtils.getTargetClass(knowledgeAnswerModel).getSimpleName())
                .isEqualTo("SpringAiKnowledgeAnswerModel");
    }

    @Test
    void retriesOneTransientProviderFailureInsideTheTotalDeadline() throws Exception {
        int requestCountBefore = PROVIDER.getRequestCount();
        PROVIDER.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"temporarily unavailable\",\"type\":\"server_error\"}}"));
        PROVIDER.enqueue(successfulProviderResponse());

        StepVerifier.create(answerKnowledgeQuestion.answer(
                        command("退款审核通过了，为什么还没到账？")))
                .assertNext(answer -> assertThat(answer.answer()).contains("1 到 5 个工作日"))
                .verifyComplete();

        assertThat(PROVIDER.getRequestCount()).isEqualTo(requestCountBefore + 2);
    }

    @Test
    void doesNotRetryTimedOutProviderResponsesOrDropCancellationErrors() throws Exception {
        int requestCountBefore = PROVIDER.getRequestCount();
        PROVIDER.enqueue(successfulProviderResponse().setBodyDelay(3, TimeUnit.SECONDS));
        Logger operatorLogger = (Logger) LoggerFactory.getLogger("reactor.core.publisher.Operators");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        operatorLogger.addAppender(appender);

        try {
            StepVerifier.create(answerKnowledgeQuestion.answer(
                            command("退款审核通过了，为什么还没到账？")))
                    .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(TimeoutException.class))
                    .verify(Duration.ofSeconds(3));
            TimeUnit.SECONDS.sleep(2);
        } finally {
            operatorLogger.detachAppender(appender);
            appender.stop();
        }

        assertThat(PROVIDER.getRequestCount()).isEqualTo(requestCountBefore + 1);
        assertThat(appender.list)
                .noneMatch(event -> event.getFormattedMessage().contains("onErrorDropped"));
    }

    @Test
    void doesNotRetryProviderRateLimits() throws Exception {
        int requestCountBefore = PROVIDER.getRequestCount();
        PROVIDER.enqueue(providerError(429, "rate_limit_exceeded"));

        StepVerifier.create(answerKnowledgeQuestion.answer(command("退款审核通过了，为什么还没到账？")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(ModelProviderException.class, providerError ->
                                assertThat(providerError.reason())
                                        .isEqualTo(ModelProviderException.Reason.RATE_LIMITED)))
                .verify();

        assertThat(PROVIDER.getRequestCount()).isEqualTo(requestCountBefore + 1);
    }

    @Test
    void mapsExhaustedProviderFailuresToAStableApplicationException() throws Exception {
        int requestCountBefore = PROVIDER.getRequestCount();
        PROVIDER.enqueue(providerError(503, "server_error"));
        PROVIDER.enqueue(providerError(503, "server_error"));

        StepVerifier.create(answerKnowledgeQuestion.answer(command("退款审核通过了，为什么还没到账？")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(ModelProviderException.class, providerError ->
                                assertThat(providerError.reason())
                                        .isEqualTo(ModelProviderException.Reason.UNAVAILABLE)))
                .verify();

        assertThat(PROVIDER.getRequestCount()).isEqualTo(requestCountBefore + 2);
    }

    @Test
    void mapsAnOpenAiCompatibleResponseWithoutCallingARealModel() throws Exception {
        PROVIDER.enqueue(successfulProviderResponse());

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
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody)
                .contains("退款审核通过了，为什么还没到账？")
                .contains("UNTRUSTED_USER_INPUT")
                .contains("AUTHORIZED_KNOWLEDGE_CONTEXT")
                .contains("knowledge-answer-v1")
                .contains("refund-policy")
                .contains("1 到 5 个工作日")
                .containsOnlyOnce("\\\"$schema\\\"");
    }

    @Test
    void rejectsAProviderResponseWithoutUsageMetadata() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String structuredAnswer = objectMapper.writeValueAsString(Map.of(
                "answer", "退款审核通过后，通常还需要 1 到 5 个工作日原路到账。",
                "citedSectionIds", List.of("arrival-time"),
                "refused", false,
                "refusalReason", ""
        ));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "id", "chatcmpl-without-usage",
                "object", "chat.completion",
                "created", 1750000000,
                "model", "fixture-model",
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", structuredAnswer),
                        "finish_reason", "stop"
                ))
        ));
        PROVIDER.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(providerResponse));

        StepVerifier.create(answerKnowledgeQuestion.answer(
                        command("退款审核通过了，为什么还没到账？")))
                .expectErrorMatches(error -> error instanceof InvalidModelAnswerException
                        && error.getMessage().contains("model usage is missing"))
                .verify();
    }

    @Test
    void rejectsAProviderResponseWithoutAnyResult() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "id", "chatcmpl-without-result",
                "object", "chat.completion",
                "created", 1750000000,
                "model", "fixture-model",
                "choices", List.of(),
                "usage", Map.of(
                        "prompt_tokens", 52,
                        "completion_tokens", 0,
                        "total_tokens", 52
                )
        ));
        PROVIDER.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(providerResponse));

        StepVerifier.create(answerKnowledgeQuestion.answer(
                        command("退款审核通过了，为什么还没到账？")))
                .expectError(InvalidModelAnswerException.class)
                .verify();
    }

    @Test
    void rejectsMalformedStructuredContent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "id", "chatcmpl-malformed-content",
                "object", "chat.completion",
                "created", 1750000000,
                "model", "fixture-model",
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", "not-json"),
                        "finish_reason", "stop"
                )),
                "usage", Map.of(
                        "prompt_tokens", 52,
                        "completion_tokens", 1,
                        "total_tokens", 53
                )
        ));
        PROVIDER.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(providerResponse));

        StepVerifier.create(answerKnowledgeQuestion.answer(
                        command("退款审核通过了，为什么还没到账？")))
                .expectError(InvalidModelAnswerException.class)
                .verify();
    }

    private static MockResponse successfulProviderResponse() throws Exception {
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
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(providerResponse);
    }

    private static MockResponse providerError(int status, String type) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"provider failure\",\"type\":\"" + type + "\"}}");
    }

    private static AnswerKnowledgeQuestionCommand command(String question) {
        return new AnswerKnowledgeQuestionCommand(
                question,
                new KnowledgeAccessScope(new TenantId("tenant-test"), "user-test", List.of()),
                Instant.parse("2026-07-13T03:00:00Z")
        );
    }
}
