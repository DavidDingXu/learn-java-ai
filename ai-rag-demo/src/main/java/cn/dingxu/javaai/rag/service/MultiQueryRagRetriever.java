package cn.dingxu.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MultiQueryRagRetriever {

    private final QueryRewriteService rewriteService;
    private final HybridRetriever hybridRetriever;

    public MultiQueryRagRetriever(QueryRewriteService rewriteService, HybridRetriever hybridRetriever) {
        this.rewriteService = rewriteService;
        this.hybridRetriever = hybridRetriever;
    }

    public MultiQueryRagRetrievalResult retrieve(String query,
                                                 List<Double> queryVector,
                                                 OperatorScope scope,
                                                 int topK) {
        QueryRewriteResult rewrite = rewriteService.rewrite(query);
        if (topK <= 0 || rewrite.queries().isEmpty()) {
            return new MultiQueryRagRetrievalResult(rewrite, List.of());
        }

        Map<String, Accumulator> merged = new LinkedHashMap<>();
        for (String rewrittenQuery : rewrite.queries()) {
            List<HybridRetrievalResult> results = hybridRetriever.search(rewrittenQuery, queryVector, scope, topK);
            for (HybridRetrievalResult result : results) {
                merged.computeIfAbsent(result.chunk().chunkId(), ignored -> new Accumulator(result.chunk()))
                        .add(result, rewrittenQuery);
            }
        }

        List<MultiQueryRetrievalItem> items = merged.values().stream()
                .map(Accumulator::toItem)
                .sorted(Comparator.comparing(MultiQueryRetrievalItem::score).reversed()
                        .thenComparing(item -> item.chunk().documentId())
                        .thenComparing(item -> item.chunk().chunkId()))
                .limit(topK)
                .toList();

        return new MultiQueryRagRetrievalResult(rewrite, items);
    }

    private static final class Accumulator {
        private final DocumentChunk chunk;
        private final Set<String> sources = new LinkedHashSet<>();
        private final List<String> matchedQueries = new ArrayList<>();
        private double score;

        private Accumulator(DocumentChunk chunk) {
            this.chunk = chunk;
        }

        private void add(HybridRetrievalResult result, String query) {
            this.score += result.score();
            this.sources.addAll(result.sources());
            if (!this.matchedQueries.contains(query)) {
                this.matchedQueries.add(query);
            }
        }

        private MultiQueryRetrievalItem toItem() {
            return new MultiQueryRetrievalItem(chunk, score, sources, matchedQueries);
        }
    }
}
