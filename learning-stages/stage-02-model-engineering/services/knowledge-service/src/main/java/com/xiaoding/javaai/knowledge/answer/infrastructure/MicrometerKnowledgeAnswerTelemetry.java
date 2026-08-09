package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.KnowledgeOperation;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerTelemetry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

final class MicrometerKnowledgeAnswerTelemetry implements KnowledgeAnswerTelemetry {

    private static final String OBSERVATION_NAME = "java.ai.knowledge.operation";

    private final ObservationRegistry registry;

    MicrometerKnowledgeAnswerTelemetry(ObservationRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <T> Mono<T> observe(KnowledgeOperation operation, Supplier<Mono<T>> publisher) {
        return Mono.defer(() -> {
            Observation observation = Observation.createNotStarted(OBSERVATION_NAME, registry)
                    .lowCardinalityKeyValue("operation", operation.name().toLowerCase())
                    .start();
            return publisher.get()
                    .doOnError(observation::error)
                    .doFinally(ignored -> observation.stop());
        });
    }
}
