package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.KnowledgeOperation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerKnowledgeAnswerTelemetryTest {

    @Test
    void recordsOnlyTheFixedOperationTag() {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        ObservationRegistry observations = ObservationRegistry.create();
        observations.observationConfig().observationHandler(new DefaultMeterObservationHandler(meters));
        MicrometerKnowledgeAnswerTelemetry telemetry = new MicrometerKnowledgeAnswerTelemetry(observations);

        StepVerifier.create(telemetry.observe(KnowledgeOperation.MODEL_CALL, () -> Mono.just("answer")))
                .expectNext("answer")
                .verifyComplete();

        assertThat(meters.get("java.ai.knowledge.operation")
                .tag("operation", "model_call")
                .timer()
                .count()).isEqualTo(1);
    }
}
