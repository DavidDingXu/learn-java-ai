package com.xiaoding.javaai.customer.consultation.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ConsultationSession {

    private final String conversationId;
    private final String tenantId;
    private final String customerId;
    private final long version;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final String summary;
    private final List<ConversationTurn> turns;
    private final Map<String, AnswerAttempt> attempts;
    private final Map<String, AnswerFeedback> feedback;

    private ConsultationSession(
            String conversationId,
            String tenantId,
            String customerId,
            long version,
            Instant createdAt,
            Instant expiresAt,
            String summary,
            List<ConversationTurn> turns,
            Map<String, AnswerAttempt> attempts,
            Map<String, AnswerFeedback> feedback
    ) {
        this.conversationId = requireText(conversationId, "conversationId");
        this.tenantId = requireText(tenantId, "tenantId");
        this.customerId = requireText(customerId, "customerId");
        this.version = version;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.summary = summary == null ? "" : summary;
        this.turns = List.copyOf(turns);
        this.attempts = Map.copyOf(attempts);
        this.feedback = Map.copyOf(feedback);
    }
    public static ConsultationSession start(
            String conversationId,
            String tenantId,
            String customerId,
            Instant now,
            Duration ttl
    ) {
        validateTime(now, ttl);
        return new ConsultationSession(conversationId, tenantId, customerId, 0,
                now, now.plus(ttl), "", List.of(), Map.of(), Map.of());
    }

    public ConsultationSession startAttempt(
            String attemptId,
            String question,
            String retryOfAttemptId,
            Instant now,
            Duration ttl
    ) {
        requireActive(now);
        validateTime(now, ttl);
        if (attempts.containsKey(attemptId)) {
            throw new IllegalStateException("answer attempt already exists: " + attemptId);
        }
        if (retryOfAttemptId != null) {
            AnswerAttempt original = requireAttempt(retryOfAttemptId);
            if (original.status() != AnswerAttemptStatus.COMPLETED) {
                throw new IllegalStateException("retry requires a completed original attempt");
            }
        }
        Map<String, AnswerAttempt> nextAttempts = new LinkedHashMap<>(attempts);
        nextAttempts.put(attemptId, AnswerAttempt.pending(attemptId, question, retryOfAttemptId, now));
        List<ConversationTurn> nextTurns = new ArrayList<>(turns);
        nextTurns.add(new ConversationTurn(ConversationRole.USER, question, attemptId, now));
        return copy(version + 1, now.plus(ttl), summary, nextTurns, nextAttempts, feedback);
    }

    public ConsultationSession completeAttempt(
            String attemptId,
            KnowledgeAnswerView answer,
            Instant now,
            Duration ttl
    ) {
        requireActive(now);
        validateTime(now, ttl);
        Map<String, AnswerAttempt> nextAttempts = new LinkedHashMap<>(attempts);
        nextAttempts.put(attemptId, requireAttempt(attemptId).complete(answer, now));
        List<ConversationTurn> nextTurns = new ArrayList<>(turns);
        String assistantText = answer.refused() ? answer.refusalReason() : answer.answer();
        nextTurns.add(new ConversationTurn(ConversationRole.ASSISTANT, assistantText, attemptId, now));
        return copy(version + 1, now.plus(ttl), summary, nextTurns, nextAttempts, feedback);
    }

    public ConsultationSession failAttempt(String attemptId, String code, Instant now, Duration ttl) {
        requireActive(now);
        validateTime(now, ttl);
        Map<String, AnswerAttempt> nextAttempts = new LinkedHashMap<>(attempts);
        nextAttempts.put(attemptId, requireAttempt(attemptId).fail(code, now));
        return copy(version + 1, now.plus(ttl), summary, turns, nextAttempts, feedback);
    }

    public ConsultationSession recordFeedback(
            String attemptId,
            FeedbackRating rating,
            String reasonCode,
            String comment,
            Instant now,
            Duration ttl
    ) {
        requireActive(now);
        validateTime(now, ttl);
        AnswerAttempt attempt = requireAttempt(attemptId);
        if (attempt.status() != AnswerAttemptStatus.COMPLETED) {
            throw new IllegalStateException("feedback requires a completed answer attempt");
        }
        Map<String, AnswerFeedback> nextFeedback = new LinkedHashMap<>(feedback);
        nextFeedback.put(attemptId, new AnswerFeedback(attemptId, rating, reasonCode, comment, now));
        return copy(version + 1, now.plus(ttl), summary, turns, attempts, nextFeedback);
    }

    public TicketHandoffSnapshot createHandoffSnapshot(
            String attemptId,
            String reasonCode,
            Instant now
    ) {
        requireActive(now);
        AnswerAttempt attempt = requireAttempt(attemptId);
        if (attempt.status() != AnswerAttemptStatus.COMPLETED) {
            throw new IllegalStateException("handoff requires a completed answer attempt");
        }
        KnowledgeAnswerView answer = attempt.answer();
        return new TicketHandoffSnapshot(
                conversationId,
                attemptId,
                tenantId,
                customerId,
                attempt.question(),
                answer.answer(),
                answer.citations(),
                answer.refusalReason(),
                feedback.get(attemptId),
                summary,
                requireText(reasonCode, "reasonCode"),
                handoffIdempotencyKey(tenantId, conversationId, attemptId),
                answer.traceId(),
                now
        );
    }

    private static String handoffIdempotencyKey(
            String tenantId,
            String conversationId,
            String attemptId
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateLengthPrefixed(digest, "tenantId");
            updateLengthPrefixed(digest, tenantId);
            updateLengthPrefixed(digest, "conversationId");
            updateLengthPrefixed(digest, conversationId);
            updateLengthPrefixed(digest, "attemptId");
            updateLengthPrefixed(digest, attemptId);
            return "handoff:v1:" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void updateLengthPrefixed(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    ConsultationSession compacted(
            String nextSummary,
            List<ConversationTurn> nextTurns,
            Instant now,
            Duration ttl
    ) {
        validateTime(now, ttl);
        if (summary.equals(nextSummary) && turns.equals(nextTurns)) return this;
        return copy(version + 1, now.plus(ttl), nextSummary, nextTurns, attempts, feedback);
    }

    public void requireOwner(String expectedTenantId, String expectedCustomerId, Instant now) {
        requireActive(now);
        if (!tenantId.equals(expectedTenantId) || !customerId.equals(expectedCustomerId)) {
            throw new SecurityException("conversation does not belong to the authenticated customer");
        }
    }

    public AnswerAttempt requireAttempt(String attemptId) {
        AnswerAttempt attempt = attempts.get(attemptId);
        if (attempt == null) throw new IllegalArgumentException("unknown answer attempt: " + attemptId);
        return attempt;
    }

    public Optional<AnswerFeedback> feedbackFor(String attemptId) {
        return Optional.ofNullable(feedback.get(attemptId));
    }

    public ConversationContextView context() {
        return new ConversationContextView(summary, turns);
    }

    public String conversationId() { return conversationId; }
    public String tenantId() { return tenantId; }
    public String customerId() { return customerId; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public String summary() { return summary; }
    public List<ConversationTurn> turns() { return turns; }
    public List<AnswerAttempt> attempts() { return List.copyOf(attempts.values()); }

    private ConsultationSession copy(
            long nextVersion,
            Instant nextExpiresAt,
            String nextSummary,
            List<ConversationTurn> nextTurns,
            Map<String, AnswerAttempt> nextAttempts,
            Map<String, AnswerFeedback> nextFeedback
    ) {
        return new ConsultationSession(conversationId, tenantId, customerId, nextVersion,
                createdAt, nextExpiresAt, nextSummary, nextTurns, nextAttempts, nextFeedback);
    }

    private void requireActive(Instant now) {
        if (!now.isBefore(expiresAt)) {
            throw new IllegalStateException("conversation has expired");
        }
    }

    private static void validateTime(Instant now, Duration ttl) {
        if (now == null) throw new IllegalArgumentException("now must not be null");
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
