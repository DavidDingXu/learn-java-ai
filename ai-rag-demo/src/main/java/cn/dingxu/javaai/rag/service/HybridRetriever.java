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
public class HybridRetriever {

    private static final int RRF_K = 60;

    private final List<DocumentChunk> chunks;
    private final VectorIndex vectorIndex;
    private final DocumentAccessFilter accessFilter;

    public HybridRetriever(VectorIndex vectorIndex) {
        this(defaultChunks(), vectorIndex);
    }

    public HybridRetriever(List<DocumentChunk> chunks, VectorIndex vectorIndex) {
        this(chunks, vectorIndex, new DocumentAccessFilter());
    }

    public HybridRetriever(List<DocumentChunk> chunks, VectorIndex vectorIndex, DocumentAccessFilter accessFilter) {
        this.chunks = chunks == null ? List.of() : List.copyOf(chunks);
        this.vectorIndex = vectorIndex;
        this.accessFilter = accessFilter == null ? new DocumentAccessFilter() : accessFilter;
    }

    public List<HybridRetrievalResult> search(String query,
                                              List<Double> queryVector,
                                              OperatorScope scope,
                                              int topK) {
        if (topK <= 0) {
            return List.of();
        }
        Map<String, Accumulator> merged = new LinkedHashMap<>();
        mergeKeywordResults(merged, query, scope, topK);
        mergeVectorResults(merged, queryVector, scope, topK);
        return merged.values().stream()
                .map(Accumulator::toResult)
                .sorted(Comparator.comparing(HybridRetrievalResult::score).reversed()
                        .thenComparing(result -> result.chunk().documentId())
                        .thenComparing(result -> result.chunk().chunkId()))
                .limit(topK)
                .toList();
    }

    private void mergeKeywordResults(Map<String, Accumulator> merged,
                                     String query,
                                     OperatorScope scope,
                                     int topK) {
        List<DocumentChunk> keywordResults = chunks.stream()
                .filter(chunk -> accessFilter.decide(chunk, scope).allowed())
                .filter(chunk -> matches(query, chunk.content()))
                .sorted(Comparator.comparing(DocumentChunk::documentId)
                        .thenComparing(DocumentChunk::chunkId))
                .limit(topK)
                .toList();

        for (int i = 0; i < keywordResults.size(); i++) {
            add(merged, keywordResults.get(i), "keyword", rrf(i + 1));
        }
    }

    private void mergeVectorResults(Map<String, Accumulator> merged,
                                    List<Double> queryVector,
                                    OperatorScope scope,
                                    int topK) {
        if (vectorIndex == null || queryVector == null || queryVector.isEmpty()) {
            return;
        }
        List<VectorSearchResult> vectorResults = vectorIndex.search(queryVector, scope, topK);
        for (int i = 0; i < vectorResults.size(); i++) {
            VectorSearchResult result = vectorResults.get(i);
            add(merged, result.chunk(), "vector", rrf(i + 1));
        }
    }

    private void add(Map<String, Accumulator> merged, DocumentChunk chunk, String source, double score) {
        merged.computeIfAbsent(chunk.chunkId(), ignored -> new Accumulator(chunk))
                .add(source, score);
    }

    private double rrf(int rank) {
        return 1.0 / (RRF_K + rank);
    }

    private boolean matches(String query, String content) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String normalized = query.replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length(); i++) {
            String token = normalized.substring(i, i + 1);
            if (!isStopToken(token) && content.contains(token)) {
                return true;
            }
        }
        return content.contains(query);
    }

    private boolean isStopToken(String token) {
        return Set.of("怎", "么", "处", "理", "的", "了", "吗", "？", "?").contains(token);
    }

    private static List<DocumentChunk> defaultChunks() {
        return List.of();
    }

    private static final class Accumulator {
        private final DocumentChunk chunk;
        private final Set<String> sources = new LinkedHashSet<>();
        private double score;

        private Accumulator(DocumentChunk chunk) {
            this.chunk = chunk;
        }

        private void add(String source, double score) {
            this.sources.add(source);
            this.score += score;
        }

        private HybridRetrievalResult toResult() {
            return new HybridRetrievalResult(chunk, score, sources);
        }
    }
}
