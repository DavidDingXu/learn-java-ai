package com.xiaoding.javaai.knowledge.answer.application;

public sealed interface AnswerStreamEvent permits
        AnswerStreamEvent.MetadataEvent,
        AnswerStreamEvent.DeltaEvent,
        AnswerStreamEvent.HeartbeatEvent,
        AnswerStreamEvent.CitationEvent,
        AnswerStreamEvent.CompletedEvent,
        AnswerStreamEvent.ErrorEvent {

    record MetadataEvent(String traceId, String promptVersion)
            implements AnswerStreamEvent {
    }

    record DeltaEvent(String text) implements AnswerStreamEvent {
    }

    record HeartbeatEvent(long epochMillis) implements AnswerStreamEvent {
    }

    record CitationEvent(Citation citation) implements AnswerStreamEvent {
    }

    record CompletedEvent(
            String model,
            ModelUsage usage,
            String finishReason,
            long ttftMillis,
            boolean refused,
            String refusalReason
    ) implements AnswerStreamEvent {
    }

    record ErrorEvent(String code, String message) implements AnswerStreamEvent {
    }
}
