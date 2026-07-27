package com.xiaoding.javaai.customer.consultation.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultationSessionTest {

    private static final Instant NOW = Instant.parse("2026-07-13T04:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    @Test
    void keeps_attempt_feedback_and_handoff_bound_to_the_same_customer_session() {
        ConsultationSession session = ConsultationSession.start(
                "conversation-1", "tenant-a", "customer-42", NOW, TTL
        );
        session = session.startAttempt("attempt-1", "退款多久到账？", null, NOW, TTL)
                .completeAttempt("attempt-1", answer(), NOW.plusSeconds(1), TTL)
                .recordFeedback("attempt-1", FeedbackRating.NOT_HELPFUL,
                        "ANSWER_INCOMPLETE", "没有说明银行卡差异", NOW.plusSeconds(2), TTL);

        TicketHandoffSnapshot snapshot = session.createHandoffSnapshot(
                "attempt-1", "CUSTOMER_REQUESTED_HUMAN", NOW.plusSeconds(3)
        );

        assertThat(session.requireAttempt("attempt-1").status())
                .isEqualTo(AnswerAttemptStatus.COMPLETED);
        assertThat(session.feedbackFor("attempt-1")).isPresent();
        assertThat(snapshot.tenantId()).isEqualTo("tenant-a");
        assertThat(snapshot.customerId()).isEqualTo("customer-42");
        assertThat(snapshot.question()).isEqualTo("退款多久到账？");
        assertThat(snapshot.citations()).extracting(CitationView::sectionId)
                .containsExactly("arrival-time");
        assertThat(snapshot.idempotencyKey())
                .matches("handoff:v1:[0-9a-f]{64}")
                .doesNotContain("tenant-a", "conversation-1", "attempt-1");
    }

    @Test
    void handoff_idempotency_key_preserves_field_boundaries() {
        ConsultationSession first = completedSession("a:b", "c", "d");
        ConsultationSession second = completedSession("a", "b:c", "d");

        String firstKey = first.createHandoffSnapshot("d", "CUSTOMER_REQUESTED_HUMAN", NOW)
                .idempotencyKey();
        String secondKey = second.createHandoffSnapshot("d", "CUSTOMER_REQUESTED_HUMAN", NOW)
                .idempotencyKey();

        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    void retry_attempt_references_the_original_attempt_instead_of_overwriting_it() {
        ConsultationSession session = ConsultationSession.start(
                        "conversation-1", "tenant-a", "customer-42", NOW, TTL)
                .startAttempt("attempt-1", "退款多久到账？", null, NOW, TTL)
                .completeAttempt("attempt-1", answer(), NOW.plusSeconds(1), TTL)
                .startAttempt("attempt-2", "退款多久到账？", "attempt-1", NOW.plusSeconds(2), TTL);

        assertThat(session.requireAttempt("attempt-1").retryOfAttemptId()).isNull();
        assertThat(session.requireAttempt("attempt-2").retryOfAttemptId()).isEqualTo("attempt-1");
        assertThat(session.attempts()).hasSize(2);
    }

    @Test
    void rejects_feedback_and_handoff_for_an_unfinished_attempt() {
        ConsultationSession session = ConsultationSession.start(
                        "conversation-1", "tenant-a", "customer-42", NOW, TTL)
                .startAttempt("attempt-1", "退款多久到账？", null, NOW, TTL);

        assertThatThrownBy(() -> session.recordFeedback(
                "attempt-1", FeedbackRating.HELPFUL, null, null, NOW, TTL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
        assertThatThrownBy(() -> session.createHandoffSnapshot(
                "attempt-1", "CUSTOMER_REQUESTED_HUMAN", NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
    }

    private static KnowledgeAnswerView answer() {
        return new KnowledgeAnswerView(
                "退款通常会在 1 到 5 个工作日到账。",
                List.of(new CitationView("refund-policy", "v1", "arrival-time", "退款到账时间")),
                false,
                null,
                "trace-123"
        );
    }

    private static ConsultationSession completedSession(
            String tenantId,
            String conversationId,
            String attemptId
    ) {
        return ConsultationSession.start(conversationId, tenantId, "customer-42", NOW, TTL)
                .startAttempt(attemptId, "退款多久到账？", null, NOW, TTL)
                .completeAttempt(attemptId, answer(), NOW.plusSeconds(1), TTL);
    }
}
