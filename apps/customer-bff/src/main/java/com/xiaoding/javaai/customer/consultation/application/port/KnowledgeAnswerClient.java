package com.xiaoding.javaai.customer.consultation.application.port;

import com.xiaoding.javaai.customer.consultation.domain.ConversationContextView;
import com.xiaoding.javaai.customer.consultation.domain.KnowledgeAnswerView;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface KnowledgeAnswerClient {
    Mono<KnowledgeAnswerView> answer(DelegatedAccessToken token, Request request);

    record Request(String question, ConversationContextView context) {
        public Request {
            if (question == null || question.isBlank()) {
                throw new IllegalArgumentException("question must not be blank");
            }
            if (context == null) throw new IllegalArgumentException("context must not be null");
            question = question.trim();
        }
    }
}
