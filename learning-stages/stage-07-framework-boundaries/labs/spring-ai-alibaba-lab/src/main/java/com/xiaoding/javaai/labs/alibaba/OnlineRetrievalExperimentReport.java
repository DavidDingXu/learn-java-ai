package com.xiaoding.javaai.labs.alibaba;

import java.time.Duration;
import java.util.List;

public record OnlineRetrievalExperimentReport(
        String caseId,
        List<String> embeddingRanking,
        List<String> rerankedRanking,
        double recallAtK,
        double mrr,
        Duration embeddingLatency,
        Duration rerankLatency) {

    public OnlineRetrievalExperimentReport {
        embeddingRanking = List.copyOf(embeddingRanking);
        rerankedRanking = List.copyOf(rerankedRanking);
        if (embeddingLatency.isNegative() || rerankLatency.isNegative()) {
            throw new IllegalArgumentException("latencies must not be negative");
        }
    }
}
