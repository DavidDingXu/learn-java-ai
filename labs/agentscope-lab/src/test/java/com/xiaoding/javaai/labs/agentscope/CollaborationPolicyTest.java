package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.event.AgentEventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollaborationPolicyTest {

    private final CollaborationPolicy policy = new CollaborationPolicy();

    @Test
    void keepsDependentReadOnlyWorkInOneAgent() {
        CollaborationDecision decision = policy.decide(List.of(
                new WorkUnit("policy-search", true, false),
                new WorkUnit("draft-answer", false, false)));

        assertEquals(CollaborationMode.SINGLE_AGENT, decision.mode());
    }

    @Test
    void usesMultipleAgentsOnlyForIndependentReadOnlyWork() {
        CollaborationDecision decision = policy.decide(List.of(
                new WorkUnit("policy-search", true, false),
                new WorkUnit("ticket-summary", true, false)));

        assertEquals(CollaborationMode.MULTI_AGENT, decision.mode());
    }

    @Test
    void highRiskSideEffectRequiresHumanEvent() {
        CollaborationDecision decision = policy.decide(List.of(
                new WorkUnit("refund-write", false, true)));

        assertEquals(CollaborationMode.HUMAN_REQUIRED, decision.mode());
        assertEquals(AgentEventType.REQUIRE_USER_CONFIRM, decision.confirmationEvent().getType());
    }
}
