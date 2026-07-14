package com.xiaoding.javaai.labs.alibaba;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalReplacementExperimentTest {

    @Test
    void evaluatesDomesticEmbeddingAndRerankAgainstTheSameGoldenSet() {
        DomesticRetrievalProfile profile = new DomesticRetrievalProfile(
                "text-embedding-v4", 1024, "gte-rerank-v2", 3);
        RetrievalReplacementExperiment experiment = new RetrievalReplacementExperiment(profile);

        RetrievalExperimentReport report = experiment.evaluate(List.of(
                new RetrievalGoldenCase("q1", List.of("d1"), List.of("d1", "d2"), Duration.ofMillis(42)),
                new RetrievalGoldenCase("q2", List.of("d3"), List.of("d4", "d3"), Duration.ofMillis(55))
        ));

        assertEquals("text-embedding-v4", experiment.embeddingOptions().getModel());
        assertEquals(1024, experiment.embeddingOptions().getDimensions());
        assertEquals("gte-rerank-v2", experiment.rerankOptions().getModel());
        assertEquals(3, experiment.rerankOptions().getTopN());
        assertEquals(1.0, report.recallAtK());
        assertEquals(0.75, report.mrr());
        assertEquals(Duration.ofMillis(55), report.p95Latency());
        assertTrue(report.passes(new RetrievalThresholds(1.0, 0.7, Duration.ofMillis(80))));
    }

    @Test
    void dimensionChangeRequiresAFullReindex() {
        DomesticRetrievalProfile oldProfile = new DomesticRetrievalProfile(
                "text-embedding-v3", 768, "gte-rerank", 3);
        DomesticRetrievalProfile newProfile = new DomesticRetrievalProfile(
                "text-embedding-v4", 1024, "gte-rerank-v2", 3);

        assertTrue(newProfile.requiresFullReindexFrom(oldProfile));
        assertFalse(newProfile.requiresFullReindexFrom(newProfile));
    }
}
