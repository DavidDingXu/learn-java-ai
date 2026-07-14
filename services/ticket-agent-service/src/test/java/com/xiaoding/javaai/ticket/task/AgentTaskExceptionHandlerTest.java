package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.agent.application.ConfirmationIdempotencyConflictException;
import com.xiaoding.javaai.ticket.agent.application.AgentCapacityExceededException;
import com.xiaoding.javaai.ticket.agent.infrastructure.AgentExternalIntegrationDisabledException;
import com.xiaoding.javaai.ticket.agent.infrastructure.AgentModelNotConfiguredException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTaskExceptionHandlerTest {

    private final AgentTaskExceptionHandler handler = new AgentTaskExceptionHandler();

    @Test
    void maps_not_found_and_access_denied_without_hiding_them_as_bad_requests() {
        assertThat(handler.notFound(new AgentTaskNotFoundException("task-404")))
                .satisfies(response -> {
                    assertThat(response.getStatusCode().value()).isEqualTo(404);
                    assertThat(response.getBody().code()).isEqualTo("AGENT_TASK_NOT_FOUND");
                });
        assertThat(handler.forbidden(new AgentTaskAccessDeniedException("different tenant")))
                .satisfies(response -> {
                    assertThat(response.getStatusCode().value()).isEqualTo(403);
                    assertThat(response.getBody().code()).isEqualTo("AGENT_TASK_ACCESS_DENIED");
                });
    }

    @Test
    void maps_idempotency_optimistic_lock_and_state_failures_to_conflict() {
        assertThat(handler.conflict(new ConfirmationIdempotencyConflictException("confirm:key"))
                .getStatusCode().value()).isEqualTo(409);
        assertThat(handler.conflict(new OptimisticTaskLockException("task-1", 1, 2))
                .getStatusCode().value()).isEqualTo(409);
        assertThat(handler.stateConflict(new IllegalStateException("confirmation has expired")))
                .satisfies(response -> {
                    assertThat(response.getStatusCode().value()).isEqualTo(409);
                    assertThat(response.getBody().code()).isEqualTo("AGENT_TASK_STATE_CONFLICT");
                });
    }

    @Test
    void exposes_disabled_external_capabilities_as_service_unavailable() {
        assertThat(handler.unavailable(new AgentModelNotConfiguredException())
                .getStatusCode().value()).isEqualTo(503);
        assertThat(handler.unavailable(new AgentExternalIntegrationDisabledException("knowledge read tool"))
                .getStatusCode().value()).isEqualTo(503);
    }

    @Test
    void maps_agent_run_capacity_to_too_many_requests() {
        assertThat(handler.capacity(new AgentCapacityExceededException())
                .getStatusCode().value()).isEqualTo(429);
        assertThat(handler.capacity(new AgentCapacityExceededException())
                .getBody().code()).isEqualTo("AGENT_RUN_CAPACITY_EXCEEDED");
    }
}
