package com.xiaoding.javaai.knowledge.answer.application.port;

import com.xiaoding.javaai.knowledge.answer.application.KnowledgeOperation;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public interface KnowledgeAnswerTelemetry {

    <T> Mono<T> observe(KnowledgeOperation operation, Supplier<Mono<T>> publisher);
}
