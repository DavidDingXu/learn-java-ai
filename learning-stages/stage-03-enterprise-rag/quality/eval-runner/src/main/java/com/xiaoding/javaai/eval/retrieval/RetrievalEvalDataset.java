package com.xiaoding.javaai.eval.retrieval;

import java.util.List;

public record RetrievalEvalDataset(
        String version,
        List<RetrievalEvalCase> cases
) {
    public RetrievalEvalDataset {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("version must not be blank");
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("cases must not be empty");
        cases = List.copyOf(cases);
    }
}
