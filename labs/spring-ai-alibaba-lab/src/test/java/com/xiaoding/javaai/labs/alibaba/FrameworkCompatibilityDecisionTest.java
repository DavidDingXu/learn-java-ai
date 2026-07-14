package com.xiaoding.javaai.labs.alibaba;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkCompatibilityDecisionTest {

    @Test
    void isolatesAlibabaLabWhenSpringAiAndBootBaselinesDiffer() {
        FrameworkBaseline mainline = new FrameworkBaseline("Spring AI", "2.0.0", "4.1.0");
        FrameworkBaseline candidate = new FrameworkBaseline("Spring AI Alibaba", "1.1.2.3", "3.5.x");

        FrameworkCompatibilityDecision decision = FrameworkCompatibilityDecision.compare(mainline, candidate);

        assertFalse(decision.inPlaceCompatible());
        assertEquals(MigrationBoundary.ISOLATED_SERVICE_OR_LAB, decision.boundary());
        assertTrue(decision.reasons().stream().anyMatch(reason -> reason.contains("Boot")));
    }
}
