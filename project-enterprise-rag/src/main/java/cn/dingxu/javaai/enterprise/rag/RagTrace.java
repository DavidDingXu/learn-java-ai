package cn.dingxu.javaai.enterprise.rag;

import java.util.List;

public record RagTrace(String traceId, List<RagTraceStep> steps) {
    public RagTrace {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
