package com.xiaoding.javaai.customer.consultation.application.port;

import com.xiaoding.javaai.customer.consultation.domain.ConsultationSession;
import reactor.core.publisher.Mono;

public interface ConsultationSessionStore {
    Mono<ConsultationSession> create(ConsultationSession session);

    Mono<ConsultationSession> findById(String conversationId);

    Mono<ConsultationSession> save(ConsultationSession session, long expectedVersion);
}
