package com.xiaoding.javaai.rag.service;

import java.util.List;
import java.util.Set;

public record MultiQueryRetrievalItem(
        DocumentChunk chunk,
        double score,
        Set<String> sources,
        List<String> matchedQueries
) {
    public MultiQueryRetrievalItem {
        if (chunk == null) {
            throw new IllegalArgumentException("chunk must not be null");
        }
        sources = sources == null ? Set.of() : Set.copyOf(sources);
        matchedQueries = matchedQueries == null ? List.of() : List.copyOf(matchedQueries);
    }
}
