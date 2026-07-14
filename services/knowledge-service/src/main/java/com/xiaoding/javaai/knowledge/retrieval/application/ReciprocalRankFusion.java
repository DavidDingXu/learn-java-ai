package com.xiaoding.javaai.knowledge.retrieval.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReciprocalRankFusion {

    private final int rankConstant;

    public ReciprocalRankFusion(int rankConstant) {
        if (rankConstant < 1) throw new IllegalArgumentException("rankConstant must be positive");
        this.rankConstant = rankConstant;
    }

    @SafeVarargs
    public final List<RetrievedKnowledgeChunk> fuse(
            List<RetrievedKnowledgeChunk> firstRanking,
            List<RetrievedKnowledgeChunk> secondRanking,
            int topK,
            List<RetrievedKnowledgeChunk>... additionalRankings
    ) {
        if (topK < 1 || topK > 100) throw new IllegalArgumentException("topK must be between 1 and 100");

        List<List<RetrievedKnowledgeChunk>> rankings = new ArrayList<>();
        rankings.add(List.copyOf(firstRanking));
        rankings.add(List.copyOf(secondRanking));
        for (List<RetrievedKnowledgeChunk> ranking : additionalRankings) rankings.add(List.copyOf(ranking));

        Map<String, RetrievedKnowledgeChunk> chunksById = new LinkedHashMap<>();
        Map<String, Double> scoresById = new HashMap<>();
        for (List<RetrievedKnowledgeChunk> ranking : rankings) {
            var seenInRanking = new HashSet<String>();
            for (int index = 0; index < ranking.size(); index += 1) {
                RetrievedKnowledgeChunk chunk = ranking.get(index);
                if (!seenInRanking.add(chunk.chunkId())) continue;
                chunksById.putIfAbsent(chunk.chunkId(), chunk);
                scoresById.merge(
                        chunk.chunkId(),
                        1.0d / (rankConstant + index + 1),
                        Double::sum
                );
            }
        }

        return scoresById.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .limit(topK)
                .map(entry -> withScore(chunksById.get(entry.getKey()), entry.getValue()))
                .toList();
    }

    private static RetrievedKnowledgeChunk withScore(RetrievedKnowledgeChunk chunk, double score) {
        return new RetrievedKnowledgeChunk(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.documentVersion(),
                chunk.headingPath(),
                chunk.clause(),
                chunk.content(),
                score
        );
    }
}
