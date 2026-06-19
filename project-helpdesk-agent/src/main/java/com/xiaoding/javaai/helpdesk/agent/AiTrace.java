package com.xiaoding.javaai.helpdesk.agent;

import java.util.List;
import java.util.UUID;

public record AiTrace(String traceId, List<TraceStep> steps) {
    public AiTrace {
        traceId = traceId == null || traceId.isBlank() ? "trace-" + UUID.randomUUID() : traceId;
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
