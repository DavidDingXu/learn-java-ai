package com.xiaoding.javaai.observability.service;

import java.math.BigDecimal;
import java.util.List;

public record AiTraceSnapshot(AiTrace trace, List<AiSpan> spans, List<AiEvent> events, BigDecimal cost) {
    public AiTraceSnapshot {
        spans = spans == null ? List.of() : List.copyOf(spans);
        events = events == null ? List.of() : List.copyOf(events);
        cost = cost == null ? BigDecimal.ZERO : cost;
    }
}
