package com.xiaoding.javaai.customer.consultation.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationWindowPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-13T04:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    @Test
    void compacts_old_turns_without_copying_old_answer_text_into_the_summary() {
        ConsultationSession session = ConsultationSession.start(
                "conversation-1", "tenant-a", "customer-42", NOW, TTL
        );
        for (int index = 1; index <= 4; index++) {
            String attemptId = "attempt-" + index;
            session = session.startAttempt(attemptId, "第 " + index + " 个退款问题", null,
                            NOW.plusSeconds(index * 2L), TTL)
                    .completeAttempt(attemptId, answer("可能过时的回答 " + index),
                            NOW.plusSeconds(index * 2L + 1), TTL);
        }

        ConversationWindowPolicy policy = new ConversationWindowPolicy(4, 160, 300);
        ConsultationSession compacted = policy.compact(session, NOW.plusSeconds(20), TTL);

        assertThat(compacted.turns()).hasSizeLessThanOrEqualTo(4);
        assertThat(compacted.summary()).contains("第 1 个退款问题", "已回答");
        assertThat(compacted.summary()).doesNotContain("可能过时的回答");
        assertThat(policy.estimatedTokens(compacted.summary(), compacted.turns()))
                .isLessThanOrEqualTo(160);
    }

    private static KnowledgeAnswerView answer(String text) {
        return new KnowledgeAnswerView(
                text,
                List.of(new CitationView("refund-policy", "v1", "arrival-time", "退款到账时间")),
                false,
                null,
                "trace-123"
        );
    }
}
