package cn.dingxu.javaai.rag.service;

import java.util.List;
import java.util.Set;

public record RerankedRetrievalResult(
        DocumentChunk chunk,
        double originalScore,
        double rerankScore,
        Set<String> sources,
        List<String> reasons
) {
    public RerankedRetrievalResult {
        if (chunk == null) {
            throw new IllegalArgumentException("chunk must not be null");
        }
        sources = sources == null ? Set.of() : Set.copyOf(sources);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
