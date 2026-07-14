package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;
import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTaskStateTest {

    @Test
    void records_a_versioned_agent_execution_instead_of_overwriting_chat_text() {
        AgentTask accepted = task();

        AgentTask running = accepted.start(Instant.parse("2026-07-13T08:01:00Z"));
        AgentTask observed = running.recordObservation(new ToolObservation(
                "QUERY_KNOWLEDGE",
                Map.of("answer", "退款通常在原路退回后到账"),
                Instant.parse("2026-07-13T08:01:01Z")));
        AgentTask completed = observed.complete(
                "已查询退款政策并生成处理建议",
                Instant.parse("2026-07-13T08:01:02Z"));

        assertThat(accepted.state()).isEqualTo(AgentTaskState.ACCEPTED);
        assertThat(completed.state()).isEqualTo(AgentTaskState.COMPLETED);
        assertThat(completed.version()).isEqualTo(3);
        assertThat(completed.observations()).hasSize(1);
        assertThat(completed.outcome()).isEqualTo("已查询退款政策并生成处理建议");
    }

    @Test
    void rejects_completing_a_task_that_has_not_started() {
        assertThatThrownBy(() -> task().complete("done", Instant.parse("2026-07-13T08:01:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RUNNING");
    }

    private static AgentTask task() {
        return AgentTask.accepted(
                "task-100",
                new DelegatedTicketIdentity(
                        "tenant-a", "customer-42", "customer-bff",
                        List.of("CUSTOMER"), List.of("retail")),
                new AgentTaskRequest(
                        "conversation-1",
                        "Resolve customer consultation",
                        Map.of("question", "退款多久到账？")),
                Instant.parse("2026-07-13T08:00:00Z"));
    }
}
