package com.xiaoding.javaai.knowledge.answer;

import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestionCommand;
import com.xiaoding.javaai.knowledge.answer.application.KnowledgeAnswer;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LiveModelSmokeIT {

    @DynamicPropertySource
    static void liveModelProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.model.chat", () -> "openai");
        registry.add("spring.ai.openai.max-retries", () -> 0);
    }

    @Autowired
    private AnswerKnowledgeQuestion answerKnowledgeQuestion;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private Path reportPath;

    @BeforeEach
    void requireSmokeConfiguration() throws IOException {
        reportPath = prepareReportPath(Path.of(requiredSystemProperty("java-ai.smoke.report-path")));
        if (apiKey.isBlank() || "replace-with-your-api-key".equals(apiKey)) {
            throw new IllegalStateException("请先在 config/application.yml 中填写 spring.ai.openai.api-key");
        }
    }

    @Test
    void callsTheConfiguredModelAndWritesReproducibleEvidence() throws IOException {
        String question = System.getProperty(
                "java-ai.smoke.question",
                "退款已经审核通过，为什么还没有到账？"
        );
        KnowledgeAnswer answer = answerKnowledgeQuestion.answer(
                        new AnswerKnowledgeQuestionCommand(
                                question,
                                new KnowledgeAccessScope(
                                        new TenantId("tenant-smoke"), "smoke-user", List.of("support")
                                ),
                                Instant.now()
                        ))
                .block(Duration.ofSeconds(90));

        assertThat(answer).isNotNull();
        assertThat(answer.answer()).isNotBlank();
        assertThat(answer.citations()).isNotEmpty();
        assertThat(answer.model()).isNotBlank();
        writeReport(answer, question, reportPath);
    }

    private static void writeReport(KnowledgeAnswer answer, String question, Path reportPath) throws IOException {
        String commit = System.getProperty("java-ai.smoke.commit", "unknown");
        String citationSummary = answer.citations().stream()
                .map(citation -> "`%s/%s#%s`".formatted(
                        citation.documentId(), citation.version(), citation.sectionId()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");

        String report = """
                # 真实模型调用验证

                Status: LIVE_MODEL

                - Executed at: %s
                - Commit: `%s`
                - Execution mode: `LIVE_MODEL`
                - Model: `%s`
                - Finish reason: `%s`
                - Prompt tokens: %d
                - Completion tokens: %d
                - Total tokens: %d
                - Trace ID: `%s`
                - Citations: %s

                ## 验证问题

                %s

                ## 模型回答

                %s
                """.formatted(
                Instant.now(),
                commit,
                answer.model(),
                answer.finishReason(),
                answer.usage().promptTokens(),
                answer.usage().completionTokens(),
                answer.usage().totalTokens(),
                answer.traceId(),
                citationSummary,
                question,
                answer.answer()
        );
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
    }

    static Path prepareReportPath(Path reportPath) throws IOException {
        Path absolutePath = reportPath.toAbsolutePath().normalize();
        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (var ignored = Files.newOutputStream(
                absolutePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            return absolutePath;
        }
    }

    private static String requiredSystemProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }

}
