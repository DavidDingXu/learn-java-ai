package com.xiaoding.javaai.rag.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RerankService {

    private static final Set<String> STOP_TOKENS = Set.of(
            "怎", "么", "处", "理", "的", "了", "吗", "？", "?", "，", ",", "。", " "
    );

    private final DocumentAccessFilter accessFilter;

    public RerankService() {
        this(new DocumentAccessFilter());
    }

    @Autowired
    public RerankService(DocumentAccessFilter accessFilter) {
        this.accessFilter = accessFilter == null ? new DocumentAccessFilter() : accessFilter;
    }

    public List<RerankedRetrievalResult> rerank(String query,
                                                List<HybridRetrievalResult> candidates,
                                                OperatorScope scope,
                                                int topK) {
        if (topK <= 0 || candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<String> queryTokens = tokenize(query);
        return candidates.stream()
                .filter(candidate -> accessFilter.decide(candidate.chunk(), scope).allowed())
                .map(candidate -> score(candidate, queryTokens))
                .sorted(Comparator.comparing(RerankedRetrievalResult::rerankScore).reversed()
                        .thenComparing(RerankedRetrievalResult::originalScore, Comparator.reverseOrder())
                        .thenComparing(result -> result.chunk().documentId())
                        .thenComparing(result -> result.chunk().chunkId()))
                .limit(topK)
                .toList();
    }

    private RerankedRetrievalResult score(HybridRetrievalResult candidate, List<String> queryTokens) {
        DocumentChunk chunk = candidate.chunk();
        LinkedHashSet<String> reasons = new LinkedHashSet<>();

        double headingScore = overlapRatio(queryTokens, String.join("", chunk.headingPath()));
        if (headingScore > 0) {
            reasons.add("heading");
        }

        double contentScore = overlapRatio(queryTokens, chunk.content());
        if (contentScore > 0) {
            reasons.add("content");
        }

        double sourceScore = candidate.sources().size() > 1 ? 1.0 : 0.0;
        if (sourceScore > 0) {
            reasons.add("multi-source");
        }

        double originalScore = candidate.score();
        double rerankScore = originalScore * 0.2
                + headingScore * 0.45
                + contentScore * 0.45
                + sourceScore * 0.1;

        return new RerankedRetrievalResult(
                chunk,
                originalScore,
                round(rerankScore),
                candidate.sources(),
                new ArrayList<>(reasons)
        );
    }

    private List<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        String normalized = query.replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length(); i++) {
            String token = normalized.substring(i, i + 1);
            if (!STOP_TOKENS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private double overlapRatio(List<String> tokens, String text) {
        if (tokens.isEmpty() || text == null || text.isBlank()) {
            return 0.0;
        }
        int matched = 0;
        for (String token : tokens) {
            if (text.contains(token)) {
                matched++;
            }
        }
        return (double) matched / tokens.size();
    }

    private double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
