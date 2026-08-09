package com.xiaoding.javaai.customer.consultation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ConversationWindowPolicy {

    private final int maxTurns;
    private final int maxEstimatedTokens;
    private final int maxSummaryChars;

    public ConversationWindowPolicy(int maxTurns, int maxEstimatedTokens, int maxSummaryChars) {
        if (maxTurns < 2) throw new IllegalArgumentException("maxTurns must be at least 2");
        if (maxEstimatedTokens < 64) throw new IllegalArgumentException("maxEstimatedTokens must be at least 64");
        if (maxSummaryChars < 64) throw new IllegalArgumentException("maxSummaryChars must be at least 64");
        this.maxTurns = maxTurns;
        this.maxEstimatedTokens = maxEstimatedTokens;
        this.maxSummaryChars = maxSummaryChars;
    }

    public ConsultationSession compact(ConsultationSession session, Instant now, Duration ttl) {
        List<ConversationTurn> retained = new ArrayList<>(session.turns());
        List<ConversationTurn> evicted = new ArrayList<>();
        while (retained.size() > maxTurns || estimatedTokens(session.summary(), retained) > maxEstimatedTokens) {
            if (retained.size() <= 2) break;
            evicted.add(retained.removeFirst());
        }
        if (evicted.isEmpty()) return session;

        String nextSummary = mergeSummary(session.summary(), evicted);
        while (estimatedTokens(nextSummary, retained) > maxEstimatedTokens && retained.size() > 2) {
            evicted.add(retained.removeFirst());
            nextSummary = mergeSummary(session.summary(), evicted);
        }
        int summaryTokenBudget = Math.max(16,
                maxEstimatedTokens - estimatedTokens("", retained));
        nextSummary = truncate(nextSummary, Math.min(maxSummaryChars, summaryTokenBudget * 3));
        return session.compacted(nextSummary, retained, now, ttl);
    }

    public int estimatedTokens(String summary, List<ConversationTurn> turns) {
        int characters = summary == null ? 0 : summary.codePointCount(0, summary.length());
        for (ConversationTurn turn : turns) {
            characters += turn.content().codePointCount(0, turn.content().length()) + 8;
        }
        return Math.max(0, (characters + 2) / 3);
    }

    private String mergeSummary(String existingSummary, List<ConversationTurn> evicted) {
        StringBuilder summary = new StringBuilder(existingSummary == null ? "" : existingSummary.trim());
        for (ConversationTurn turn : evicted) {
            if (turn.role() != ConversationRole.USER) continue;
            if (!summary.isEmpty()) summary.append('\n');
            summary.append("用户问：").append(truncate(turn.content(), 80));
            boolean answered = evicted.stream().anyMatch(candidate ->
                    candidate.role() == ConversationRole.ASSISTANT
                            && candidate.attemptId().equals(turn.attemptId()));
            summary.append(answered ? "；结果：已回答" : "；结果：未完成");
        }
        return truncate(summary.toString(), maxSummaryChars);
    }

    private static String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) return value == null ? "" : value;
        return value.substring(0, Math.max(0, maxChars - 1)) + "…";
    }
}
