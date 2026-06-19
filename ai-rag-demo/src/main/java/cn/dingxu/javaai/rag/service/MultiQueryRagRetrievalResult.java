package cn.dingxu.javaai.rag.service;

import java.util.List;

public record MultiQueryRagRetrievalResult(
        QueryRewriteResult rewrite,
        List<MultiQueryRetrievalItem> results
) {
    public MultiQueryRagRetrievalResult {
        if (rewrite == null) {
            throw new IllegalArgumentException("rewrite must not be null");
        }
        results = results == null ? List.of() : List.copyOf(results);
    }
}
