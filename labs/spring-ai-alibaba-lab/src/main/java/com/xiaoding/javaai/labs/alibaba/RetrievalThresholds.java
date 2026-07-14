package com.xiaoding.javaai.labs.alibaba;

import java.time.Duration;

public record RetrievalThresholds(double minRecallAtK, double minMrr, Duration maxP95Latency) {
    public RetrievalThresholds {
        if (minRecallAtK < 0 || minRecallAtK > 1 || minMrr < 0 || minMrr > 1) {
            throw new IllegalArgumentException("quality thresholds must be in [0, 1]");
        }
        if (maxP95Latency == null || maxP95Latency.isNegative()) {
            throw new IllegalArgumentException("maxP95Latency must not be negative");
        }
    }
}
