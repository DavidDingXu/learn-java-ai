package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.port.TraceIdProvider;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;

final class MicrometerTraceIdProvider implements TraceIdProvider {

    private final ObjectProvider<Tracer> tracerProvider;

    MicrometerTraceIdProvider(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    public String currentTraceId() {
        Tracer tracer = tracerProvider.getIfAvailable();
        Span span = tracer == null ? null : tracer.currentSpan();
        return span == null ? "untraced" : span.context().traceId();
    }
}
