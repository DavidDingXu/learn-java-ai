package com.xiaoding.javaai.knowledge.answer;

import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestionCommand;
import com.xiaoding.javaai.knowledge.answer.application.ModelNotConfiguredException;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

@ActiveProfiles("local-lite")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LocalDisabledModelTest {

    @Autowired
    private AnswerKnowledgeQuestion answerKnowledgeQuestion;

    @Test
    void failsExplicitlyWhenTheDefaultProfileHasNoModel() {
        StepVerifier.create(answerKnowledgeQuestion.answer(
                        new AnswerKnowledgeQuestionCommand(
                                "退款为什么还没到账？",
                                new KnowledgeAccessScope(
                                        new TenantId("tenant-test"), "user-test", List.of()
                                ),
                                Instant.parse("2026-07-13T03:00:00Z")
                        )))
                .expectError(ModelNotConfiguredException.class)
                .verify();
    }
}
