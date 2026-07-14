package com.xiaoding.javaai.eval.retrieval;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalEvaluatorTest {

    @Test
    void evaluatesRealRankingsAgainstThresholdsAndKeepsBadCases() {
        RetrievalEvalDataset dataset = new RetrievalEvalDataset("retrieval-v1", List.of(
                new RetrievalEvalCase("arrival", "退款多久到账？", Set.of("chunk-arrival")),
                new RetrievalEvalCase("scope", "哪些情况不适用？", Set.of("chunk-scope"))
        ));
        RetrievalEvaluationClient client = (baseUrl, bearerToken, question, topK) ->
                question.contains("多久")
                        ? new RetrievalClientResult("embedding-v1", List.of("chunk-arrival", "chunk-other"), 80)
                        : new RetrievalClientResult("embedding-v1", List.of("chunk-other", "chunk-other"), 120);

        RetrievalEvaluationReport report = new RetrievalEvaluator(
                client,
                Clock.fixed(Instant.parse("2026-07-13T04:00:00Z"), ZoneOffset.UTC)
        ).evaluate(
                dataset,
                URI.create("https://knowledge.example.test"),
                "delegated-token",
                2,
                "commit-123",
                new RetrievalThresholds(0.5, 0.5, 0.5, 0.3, 150)
        );

        assertEquals(0.5, report.metrics().recallAtK());
        assertEquals(0.5, report.metrics().hitRateAtK());
        assertEquals(0.5, report.metrics().meanReciprocalRank());
        assertEquals(0.25, report.metrics().duplicateRateAtK());
        assertEquals(120, report.p95LatencyMillis());
        assertEquals(List.of("scope"), report.metrics().failedCaseIds());
        assertTrue(report.passed());
        assertEquals(Set.of("embedding-v1"), report.embeddingModels());
    }

    @Test
    void failsTheGateWhenOneRunReturnsMultipleEmbeddingModels() {
        RetrievalEvalDataset dataset = new RetrievalEvalDataset("retrieval-v1", List.of(
                new RetrievalEvalCase("first", "问题一", Set.of("chunk-1")),
                new RetrievalEvalCase("second", "问题二", Set.of("chunk-2"))
        ));
        RetrievalEvaluationClient client = (baseUrl, token, question, topK) ->
                question.endsWith("一")
                        ? new RetrievalClientResult("embedding-v1", List.of("chunk-1"), 20)
                        : new RetrievalClientResult("embedding-v2", List.of("chunk-2"), 20);

        RetrievalEvaluationReport report = new RetrievalEvaluator(client, Clock.systemUTC()).evaluate(
                dataset,
                URI.create("https://knowledge.example.test"),
                "delegated-token",
                1,
                "commit-123",
                new RetrievalThresholds(1, 1, 1, 0, 100)
        );

        assertTrue(!report.passed());
        assertEquals(Set.of("embedding-v1", "embedding-v2"), report.embeddingModels());
    }

    @Test
    void rejectsAnInvalidTopKBeforeCallingTheEnvironment() {
        AtomicInteger calls = new AtomicInteger();
        RetrievalEvaluationClient client = (baseUrl, token, question, topK) -> {
            calls.incrementAndGet();
            return new RetrievalClientResult("embedding-v1", List.of(), 1);
        };
        RetrievalEvalDataset dataset = new RetrievalEvalDataset("retrieval-v1", List.of(
                new RetrievalEvalCase("case-1", "问题", Set.of("chunk-1"))
        ));

        assertThrows(IllegalArgumentException.class, () ->
                new RetrievalEvaluator(client, Clock.systemUTC()).evaluate(
                        dataset,
                        URI.create("https://knowledge.example.test"),
                        "delegated-token",
                        0,
                        "commit-123",
                        new RetrievalThresholds(0, 0, 0, 1, 100)
                ));
        assertEquals(0, calls.get());
    }
}
