package com.xiaoding.javaai.eval.retrieval;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RetrievalMetricsCalculator {

    public RetrievalMetrics calculate(List<RetrievalEvalResult> cases, int k) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("cases must not be empty");
        if (k < 1 || k > 100) throw new IllegalArgumentException("k must be between 1 and 100");

        double recallTotal = 0;
        double hitTotal = 0;
        double reciprocalRankTotal = 0;
        double duplicateRateTotal = 0;
        List<String> failedCaseIds = new ArrayList<>();

        for (RetrievalEvalResult evalCase : cases) {
            List<String> topK = rawTopK(evalCase.retrievedChunkIds(), k);
            Set<String> uniqueTopK = new LinkedHashSet<>(topK);
            long relevantCount = uniqueTopK.stream().filter(evalCase.expectedChunkIds()::contains).count();
            double recall = (double) relevantCount / evalCase.expectedChunkIds().size();
            recallTotal += recall;
            if (!topK.isEmpty()) {
                duplicateRateTotal += (double) (topK.size() - uniqueTopK.size()) / topK.size();
            }

            int firstRelevantRank = firstRelevantRank(topK, evalCase.expectedChunkIds());
            if (firstRelevantRank > 0) {
                hitTotal += 1;
                reciprocalRankTotal += 1.0d / firstRelevantRank;
            }
            if (recall < 1.0d) failedCaseIds.add(evalCase.caseId());
        }

        int caseCount = cases.size();
        return new RetrievalMetrics(
                k,
                recallTotal / caseCount,
                hitTotal / caseCount,
                reciprocalRankTotal / caseCount,
                duplicateRateTotal / caseCount,
                failedCaseIds
        );
    }

    private static List<String> rawTopK(List<String> retrievedChunkIds, int k) {
        List<String> topK = new ArrayList<>(k);
        for (String chunkId : retrievedChunkIds) {
            if (chunkId == null || chunkId.isBlank()) continue;
            topK.add(chunkId);
            if (topK.size() == k) break;
        }
        return List.copyOf(topK);
    }

    private static int firstRelevantRank(List<String> retrieved, Set<String> expected) {
        for (int index = 0; index < retrieved.size(); index += 1) {
            if (expected.contains(retrieved.get(index))) return index + 1;
        }
        return 0;
    }
}
