package com.xiaoding.javaai.eval.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class RetrievalMetricsCalculatorTest {

    @Test
    void calculates_macro_recall_hit_rate_and_mrr_without_hiding_failed_cases() {
        List<RetrievalEvalResult> cases = List.of(
                new RetrievalEvalResult(
                        "refund-arrival",
                        Set.of("chunk-10", "chunk-11"),
                        List.of("chunk-10", "chunk-99", "chunk-11")
                ),
                new RetrievalEvalResult(
                        "refund-rejected",
                        Set.of("chunk-20"),
                        List.of("chunk-99", "chunk-20", "chunk-21")
                ),
                new RetrievalEvalResult(
                        "invoice-change",
                        Set.of("chunk-30"),
                        List.of("chunk-40", "chunk-41", "chunk-42")
                )
        );

        RetrievalMetrics metrics = new RetrievalMetricsCalculator().calculate(cases, 3);

        assertEquals(2.0d / 3.0d, metrics.recallAtK(), 0.000001d);
        assertEquals(2.0d / 3.0d, metrics.hitRateAtK(), 0.000001d);
        assertEquals(0.5d, metrics.meanReciprocalRank(), 0.000001d);
        assertEquals(0.0d, metrics.duplicateRateAtK(), 0.000001d);
        assertIterableEquals(List.of("invoice-change"), metrics.failedCaseIds());
    }

    @Test
    void deduplicates_retrieved_chunk_ids_before_scoring() {
        RetrievalMetrics metrics = new RetrievalMetricsCalculator().calculate(
                List.of(new RetrievalEvalResult(
                        "duplicate-results",
                        Set.of("chunk-1", "chunk-2"),
                        List.of("chunk-1", "chunk-1", "chunk-2")
                )),
                2
        );

        assertEquals(0.5d, metrics.recallAtK(), 0.000001d);
        assertEquals(1.0d, metrics.meanReciprocalRank(), 0.000001d);
        assertEquals(0.5d, metrics.duplicateRateAtK(), 0.000001d);
    }
}
