package com.xiaoding.javaai.eval.retrieval;

public record RetrievalThresholds(
        double minimumRecallAtK,
        double minimumHitRateAtK,
        double minimumMeanReciprocalRank,
        double maximumDuplicateRateAtK,
        long maximumP95LatencyMillis
) {
    public RetrievalThresholds {
        requireRatio(minimumRecallAtK, "minimumRecallAtK");
        requireRatio(minimumHitRateAtK, "minimumHitRateAtK");
        requireRatio(minimumMeanReciprocalRank, "minimumMeanReciprocalRank");
        requireRatio(maximumDuplicateRateAtK, "maximumDuplicateRateAtK");
        if (maximumP95LatencyMillis < 1) {
            throw new IllegalArgumentException("maximumP95LatencyMillis must be positive");
        }
    }

    boolean accepts(RetrievalMetrics metrics, long p95LatencyMillis) {
        return metrics.recallAtK() >= minimumRecallAtK
                && metrics.hitRateAtK() >= minimumHitRateAtK
                && metrics.meanReciprocalRank() >= minimumMeanReciprocalRank
                && metrics.duplicateRateAtK() <= maximumDuplicateRateAtK
                && p95LatencyMillis <= maximumP95LatencyMillis;
    }

    private static void requireRatio(double value, String field) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }
}
