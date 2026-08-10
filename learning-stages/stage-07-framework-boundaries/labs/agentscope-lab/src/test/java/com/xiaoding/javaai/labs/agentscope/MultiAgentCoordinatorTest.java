package com.xiaoding.javaai.labs.agentscope;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiAgentCoordinatorTest {

    @Test
    void executes_independent_specialists_and_synthesizes_their_outputs() {
        CollaborationAgent policy = agent("policy-agent", "退款通常一到五个工作日到账");
        CollaborationAgent ticket = agent("ticket-agent", "工单 T-100 状态为 OPEN");
        CollaborationAgent synthesizer = new CollaborationAgent() {
            @Override
            public String name() {
                return "summary-agent";
            }

            @Override
            public String call(String request) {
                assertTrue(request.contains("policy-agent"));
                assertTrue(request.contains("ticket-agent"));
                return "工单仍在处理中，退款预计一到五个工作日到账。";
            }
        };

        MultiAgentResult result = new MultiAgentCoordinator(Duration.ofSeconds(1))
                .execute("核对工单 T-100 的退款进度", List.of(policy, ticket), synthesizer);

        assertEquals(MultiAgentStatus.COMPLETED, result.status());
        assertEquals(2, result.specialists().size());
        assertEquals("工单仍在处理中，退款预计一到五个工作日到账。", result.answer());
    }

    @Test
    void stops_before_synthesis_when_a_required_specialist_fails() {
        AtomicInteger synthesisCalls = new AtomicInteger();
        CollaborationAgent failed = new CollaborationAgent() {
            @Override
            public String name() {
                return "ticket-agent";
            }

            @Override
            public String call(String request) {
                throw new IllegalStateException("ticket service unavailable");
            }
        };
        CollaborationAgent synthesizer = new CollaborationAgent() {
            @Override
            public String name() {
                return "summary-agent";
            }

            @Override
            public String call(String request) {
                synthesisCalls.incrementAndGet();
                return "must not run";
            }
        };

        MultiAgentResult result = new MultiAgentCoordinator(Duration.ofSeconds(1))
                .execute("核对工单", List.of(agent("policy-agent", "policy"), failed), synthesizer);

        assertEquals(MultiAgentStatus.HUMAN_REQUIRED, result.status());
        assertEquals(0, synthesisCalls.get());
        assertTrue(result.reason().contains("ticket-agent"));
        assertTrue(result.reason().contains("ticket service unavailable"));
    }

    @Test
    void cancels_a_slow_specialist_and_requires_human_review() {
        CollaborationAgent slow = new CollaborationAgent() {
            @Override
            public String name() {
                return "ticket-agent";
            }

            @Override
            public String call(String request) {
                try {
                    Thread.sleep(5000);
                    return "must not complete";
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted", error);
                }
            }
        };

        MultiAgentResult result = new MultiAgentCoordinator(Duration.ofMillis(50))
                .execute("核对工单", List.of(agent("policy-agent", "policy"), slow),
                        agent("summary-agent", "must not run"));

        assertEquals(MultiAgentStatus.HUMAN_REQUIRED, result.status());
        assertTrue(result.reason().contains("ticket-agent"));
        assertTrue(result.reason().contains("timed out after 50ms"));
    }

    private static CollaborationAgent agent(String name, String answer) {
        return new CollaborationAgent() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String call(String request) {
                return answer;
            }
        };
    }
}
