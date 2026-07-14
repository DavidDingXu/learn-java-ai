package com.xiaoding.javaai.customer.consultation.application;

import com.xiaoding.javaai.customer.consultation.domain.CitationView;

public sealed interface CustomerStreamEvent permits
        CustomerStreamEvent.SessionStarted,
        CustomerStreamEvent.Metadata,
        CustomerStreamEvent.Delta,
        CustomerStreamEvent.Heartbeat,
        CustomerStreamEvent.Citation,
        CustomerStreamEvent.Completed,
        CustomerStreamEvent.Error {

    record SessionStarted(String conversationId, String attemptId, String retryOfAttemptId)
            implements CustomerStreamEvent {
    }

    record Metadata(String traceId) implements CustomerStreamEvent {
    }

    record Delta(String text) implements CustomerStreamEvent {
    }

    record Heartbeat(long epochMillis) implements CustomerStreamEvent {
    }

    record Citation(CitationView citation) implements CustomerStreamEvent {
    }

    record Completed() implements CustomerStreamEvent {
    }

    record Error(String code, String message) implements CustomerStreamEvent {
    }
}
