package com.xiaoding.javaai.knowledge.retrieval.application;

public record RetrievalPlan(
        boolean rewriteQuery,
        boolean lexicalSearch,
        boolean rerank,
        int candidateK
) {
    public RetrievalPlan {
        if (candidateK < 1 || candidateK > 100) {
            throw new IllegalArgumentException("candidateK must be between 1 and 100");
        }
    }
}
