package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.ToolExecutionRejectedException;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.task.AgentTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryLegacyWriteToolExecutorTest {

    @Test
    void keeps_the_same_result_for_repeated_idempotency_keys() {
        InMemoryLegacyWriteToolExecutor executor = new InMemoryLegacyWriteToolExecutor();
        AgentTask task = mock(AgentTask.class);
        ConfirmationRequest confirmation = confirmation("action-100");

        ToolExecutionReceipt first = executor.execute(task, confirmation, "write:task-100:1");
        ToolExecutionReceipt duplicate = executor.execute(task, confirmation, "write:task-100:1");

        assertThat(first.duplicate()).isFalse();
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(duplicate.auditId()).isEqualTo(first.auditId());
    }

    @Test
    void rejects_reusing_a_key_for_another_action() {
        InMemoryLegacyWriteToolExecutor executor = new InMemoryLegacyWriteToolExecutor();
        AgentTask task = mock(AgentTask.class);
        executor.execute(task, confirmation("action-100"), "write:task-100:1");

        assertThatThrownBy(() -> executor.execute(
                task, confirmation("action-200"), "write:task-100:1"))
                .isInstanceOf(ToolExecutionRejectedException.class)
                .hasMessageContaining("idempotency key");
    }

    private static ConfirmationRequest confirmation(String actionId) {
        ConfirmationRequest confirmation = mock(ConfirmationRequest.class);
        when(confirmation.actionId()).thenReturn(actionId);
        return confirmation;
    }
}
