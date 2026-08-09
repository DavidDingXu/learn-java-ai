package com.xiaoding.javaai.labs.alibaba;

import java.time.Duration;

public record RetrievalExperimentReport(double recallAtK, double mrr, Duration p95Latency, int caseCount) {
    public boolean passes(RetrievalThresholds thresholds) {
        return recallAtK >= thresholds.minRecallAtK()
                && mrr >= thresholds.minMrr()
                && p95Latency.compareTo(thresholds.maxP95Latency()) <= 0;
    }
}
