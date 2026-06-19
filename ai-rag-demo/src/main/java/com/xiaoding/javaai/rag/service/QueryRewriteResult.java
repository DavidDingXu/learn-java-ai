package com.xiaoding.javaai.rag.service;

import java.util.List;

public record QueryRewriteResult(
        String originalQuery,
        List<String> queries,
        List<String> reasons
) {
    public QueryRewriteResult {
        originalQuery = originalQuery == null ? "" : originalQuery;
        queries = queries == null ? List.of() : List.copyOf(queries);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
