package com.xiaoding.javaai.labs.agentscope;

import java.time.Duration;
import java.util.List;

public final class MultiAgentFailureApplication {

    private MultiAgentFailureApplication() {
    }

    public static void main(String[] args) {
        CollaborationAgent policyAgent = fixed("policy-agent", "审核通过后一到五个工作日到账");
        CollaborationAgent unavailableTicketAgent = new CollaborationAgent() {
            @Override
            public String name() {
                return "ticket-agent";
            }

            @Override
            public String call(String request) {
                throw new IllegalStateException("ticket service unavailable");
            }
        };
        CollaborationAgent synthesizer = fixed("summary-agent", "this answer must never be returned");

        MultiAgentResult result = new MultiAgentCoordinator(Duration.ofSeconds(2)).execute(
                "核对工单 T-100 的退款进度",
                List.of(policyAgent, unavailableTicketAgent),
                synthesizer);
        System.out.printf("status=%s answer=%s reason=%s%n",
                result.status(), result.answer(), result.reason());
    }

    private static CollaborationAgent fixed(String name, String answer) {
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
