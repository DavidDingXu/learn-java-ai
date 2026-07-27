package com.xiaoding.javaai.customer.consultation.application.port;

import com.xiaoding.javaai.customer.consultation.domain.CitationView;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import reactor.core.publisher.Flux;

public interface KnowledgeAnswerStreamClient {
    Flux<Event> stream(DelegatedAccessToken token, KnowledgeAnswerClient.Request request);

    sealed interface Event permits Metadata, Delta, Heartbeat, Citation, Completed, Error {
    }

    record Metadata(String traceId) implements Event {
    }

    record Delta(String text) implements Event {
    }

    record Heartbeat(long epochMillis) implements Event {
    }

    record Citation(CitationView citation) implements Event {
    }

    record Completed(boolean refused, String refusalReason) implements Event {
    }

    record Error(String code, String message) implements Event {
    }
}
