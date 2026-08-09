package com.xiaoding.javaai.eval.retrieval;

import java.util.List;

public record RetrievalMetrics(
        int k,
        double recallAtK,
        double hitRateAtK,
        double meanReciprocalRank,
        double duplicateRateAtK,
        List<String> failedCaseIds
) {
    public RetrievalMetrics {
        failedCaseIds = List.copyOf(failedCaseIds);
    }
}
