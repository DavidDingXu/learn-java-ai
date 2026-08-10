package com.xiaoding.javaai.labs.alibaba;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineRetrievalReplacementExperimentTest {

    @Test
    void ranks_with_returned_embeddings_and_then_calls_the_reranker() {
        TextEmbeddingGateway embedding = inputs -> inputs.stream()
                .map(input -> switch (input) {
                    case "退款到账时间" -> new float[]{1, 0};
                    case "退款审核后原路退回，通常一到五个工作日到账" -> new float[]{0.8f, 0.2f};
                    case "电子发票开具规则" -> new float[]{0, 1};
                    default -> new float[]{0.2f, 0.8f};
                })
                .toList();
        TextRerankGateway reranker = (query, candidates) -> List.of(
                new ScoredRetrievalCandidate("refund-policy", 0.97),
                new ScoredRetrievalCandidate("shipping-policy", 0.20),
                new ScoredRetrievalCandidate("invoice-policy", 0.05));
        OnlineRetrievalReplacementExperiment experiment =
                new OnlineRetrievalReplacementExperiment(embedding, reranker);

        OnlineRetrievalExperimentReport report = experiment.run(
                "refund-arrival",
                "退款到账时间",
                List.of(
                        new RetrievalCandidate("refund-policy", "退款审核后原路退回，通常一到五个工作日到账"),
                        new RetrievalCandidate("invoice-policy", "电子发票开具规则"),
                        new RetrievalCandidate("shipping-policy", "物流签收和拒收规则")),
                List.of("refund-policy"));

        assertEquals("refund-policy", report.embeddingRanking().getFirst());
        assertEquals(List.of("refund-policy", "shipping-policy", "invoice-policy"), report.rerankedRanking());
        assertEquals(1.0, report.recallAtK());
        assertEquals(1.0, report.mrr());
        assertTrue(report.embeddingLatency().compareTo(Duration.ZERO) >= 0);
        assertTrue(report.rerankLatency().compareTo(Duration.ZERO) >= 0);
    }

    @Test
    void rejects_provider_results_that_drop_or_invent_candidates() {
        OnlineRetrievalReplacementExperiment experiment = new OnlineRetrievalReplacementExperiment(
                inputs -> inputs.stream().map(ignored -> new float[]{1, 0}).toList(),
                (query, candidates) -> List.of(new ScoredRetrievalCandidate("invented", 1.0)));

        IllegalArgumentException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> experiment.run(
                        "case-1",
                        "退款",
                        List.of(new RetrievalCandidate("refund-policy", "退款规则")),
                        List.of("refund-policy")));

        assertTrue(error.getMessage().contains("same candidate ids"));
    }
}
