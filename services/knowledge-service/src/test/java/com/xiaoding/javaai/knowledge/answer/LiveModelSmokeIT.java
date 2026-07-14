package com.xiaoding.javaai.knowledge.answer;

import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestionCommand;
import com.xiaoding.javaai.knowledge.answer.application.ExecutionMode;
import com.xiaoding.javaai.knowledge.answer.application.KnowledgeAnswer;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("live-model")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LiveModelSmokeIT {

    @Autowired
    private AnswerKnowledgeQuestion answerKnowledgeQuestion;

    @Test
    void callsTheConfiguredModelAndWritesRedactedEvidence() throws IOException {
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
        assertThat(answer.executionMode()).isEqualTo(ExecutionMode.LIVE_MODEL);

        writeReport(answer, question);
    }

    private static void writeReport(KnowledgeAnswer answer, String question) throws IOException {
        Path reportPath = Path.of(requiredSystemProperty("java-ai.smoke.report-path"));
        Path parent = reportPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String commit = System.getProperty("java-ai.smoke.commit", "unknown");
        String citationSummary = answer.citations().stream()
                .map(citation -> "`%s/%s#%s`".formatted(
                        citation.documentId(), citation.version(), citation.sectionId()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");

        String report = """
                # Lesson 04 Live Model Smoke

                Status: LIVE_MODEL

                - Executed at: %s
                - Commit: `%s`
                - Execution mode: `%s`
                - Model: `%s`
                - Finish reason: `%s`
                - Prompt tokens: %d
                - Completion tokens: %d
                - Total tokens: %d
                - Trace ID: `%s`
                - Citations: %s

                ## Question

                %s

                ## Answer

                %s

                ## Redaction

                The API key and base URL are never written to this report.
                """.formatted(
                Instant.now(),
                commit,
                answer.executionMode(),
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

    private static String requiredSystemProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }
}
