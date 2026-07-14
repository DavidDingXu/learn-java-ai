package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.application.port.ConsultationSessionStore;
import com.xiaoding.javaai.customer.consultation.domain.ConsultationSession;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class InMemoryConsultationSessionStore implements ConsultationSessionStore {

    private final ConcurrentMap<String, ConsultationSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Mono<ConsultationSession> create(ConsultationSession session) {
        return Mono.fromSupplier(() -> {
            ConsultationSession existing = sessions.putIfAbsent(session.conversationId(), session);
            if (existing != null) {
                throw new IllegalStateException("conversation already exists: " + session.conversationId());
            }
            return session;
        });
    }

    @Override
    public Mono<ConsultationSession> findById(String conversationId) {
        return Mono.justOrEmpty(sessions.get(conversationId));
    }

    @Override
    public Mono<ConsultationSession> save(ConsultationSession session, long expectedVersion) {
        return Mono.fromSupplier(() -> {
            AtomicReference<RuntimeException> failure = new AtomicReference<>();
            sessions.compute(session.conversationId(), (id, current) -> {
                if (current == null) {
                    failure.set(new IllegalStateException("unknown conversation: " + id));
                    return null;
                }
                if (current.version() != expectedVersion) {
                    failure.set(new IllegalStateException(
                            "conversation version conflict: expected " + expectedVersion
                                    + " but was " + current.version()));
                    return current;
                }
                if (session.version() <= expectedVersion) {
                    failure.set(new IllegalArgumentException("new session version must advance"));
                    return current;
                }
                return session;
            });
            if (failure.get() != null) throw failure.get();
            return session;
        });
    }
}
