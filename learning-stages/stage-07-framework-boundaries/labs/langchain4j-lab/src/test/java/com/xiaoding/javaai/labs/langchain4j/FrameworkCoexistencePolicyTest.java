package com.xiaoding.javaai.labs.langchain4j;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrameworkCoexistencePolicyTest {

    @Test
    void routesByBusinessCapabilityInsteadOfSharingFrameworkObjects() {
        FrameworkCoexistencePolicy policy = new FrameworkCoexistencePolicy(Map.of(
                "knowledge-answer", FrameworkChoice.SPRING_AI,
                "ticket-decision-experiment", FrameworkChoice.LANGCHAIN4J));

        assertEquals(FrameworkChoice.SPRING_AI, policy.frameworkFor("knowledge-answer"));
        assertEquals(FrameworkChoice.LANGCHAIN4J, policy.frameworkFor("ticket-decision-experiment"));
        assertThrows(IllegalArgumentException.class, () -> policy.frameworkFor("unknown"));
    }
}
