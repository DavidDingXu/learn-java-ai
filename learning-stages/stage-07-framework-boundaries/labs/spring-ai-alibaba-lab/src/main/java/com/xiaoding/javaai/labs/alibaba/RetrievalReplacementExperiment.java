package com.xiaoding.javaai.labs.alibaba;

import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RetrievalReplacementExperiment {

    private final DomesticRetrievalProfile profile;

    public RetrievalReplacementExperiment(DomesticRetrievalProfile profile) {
        this.profile = profile;
    }

    public DashScopeEmbeddingOptions embeddingOptions() {
        return DashScopeEmbeddingOptions.builder()
                .model(profile.embeddingModel())
                .dimensions(profile.dimensions())
                .textType("document")
                .build();
    }

    public DashScopeRerankOptions rerankOptions() {
        return DashScopeRerankOptions.builder()
                .model(profile.rerankModel())
                .topN(profile.rerankTopN())
                .returnDocuments(false)
                .build();
    }

    public RetrievalExperimentReport evaluate(List<RetrievalGoldenCase> cases) {
        if (cases == null || cases.isEmpty()) {
            throw new IllegalArgumentException("cases must not be empty");
        }
        double recall = cases.stream().mapToDouble(RetrievalReplacementExperiment::recall).average().orElseThrow();
        double mrr = cases.stream().mapToDouble(RetrievalReplacementExperiment::reciprocalRank).average().orElseThrow();
        List<Duration> latencies = cases.stream()
                .map(RetrievalGoldenCase::latency)
                .sorted(Comparator.naturalOrder())
                .toList();
        int p95Index = Math.max(0, (int) Math.ceil(latencies.size() * 0.95) - 1);
        return new RetrievalExperimentReport(recall, mrr, latencies.get(p95Index), cases.size());
    }

    private static double recall(RetrievalGoldenCase testCase) {
        Set<String> retrieved = new HashSet<>(testCase.retrievedDocumentIds());
        long matched = testCase.expectedDocumentIds().stream().filter(retrieved::contains).count();
        return (double) matched / testCase.expectedDocumentIds().size();
    }

    private static double reciprocalRank(RetrievalGoldenCase testCase) {
        Set<String> expected = new HashSet<>(testCase.expectedDocumentIds());
        for (int index = 0; index < testCase.retrievedDocumentIds().size(); index++) {
            if (expected.contains(testCase.retrievedDocumentIds().get(index))) {
                return 1.0 / (index + 1);
            }
        }
        return 0;
    }
}
