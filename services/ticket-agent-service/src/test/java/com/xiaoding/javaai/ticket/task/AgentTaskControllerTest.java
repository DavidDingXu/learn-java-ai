package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTaskControllerTest {

    @Test
    void accepts_a_versioned_agent_task_with_an_idempotency_key() {
        AgentTaskController controller = new AgentTaskController(
                new AgentTaskIntakeService(
                        () -> "task-100",
                        new InMemoryAgentTaskRepository(),
                        Clock.fixed(Instant.parse("2026-07-13T08:00:00Z"), ZoneOffset.UTC)),
                new DelegatedTicketIdentityFactory());

        AgentTaskReceipt receipt = controller.create(
                jwt(),
                "handoff:tenant-a:conversation-1:attempt-1",
                new AgentTaskWebRequest(
                        "conversation-1",
                        "Resolve customer consultation",
                        Map.of("source", "customer-consultation")))
                .getBody();

        assertThat(receipt.taskId()).isEqualTo("task-100");
        assertThat(receipt.status()).isEqualTo("ACCEPTED");
    }

    private static Jwt jwt() {
        return new Jwt(
                "token-value",
                Instant.parse("2026-07-13T08:00:00Z"),
                Instant.parse("2026-07-13T08:05:00Z"),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "customer-42",
                        "tenantId", "tenant-a",
                        "act", Map.of("sub", "customer-bff"),
                        "roles", java.util.List.of("CUSTOMER"),
                        "departments", java.util.List.of("retail")
                ));
    }
}
