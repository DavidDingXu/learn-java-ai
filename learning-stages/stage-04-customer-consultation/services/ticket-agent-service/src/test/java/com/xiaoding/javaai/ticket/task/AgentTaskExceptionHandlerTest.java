package com.xiaoding.javaai.ticket.task;

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
        assertThat(handler.conflict(new IdempotencyConflictException("handoff:key"))
                .getStatusCode().value()).isEqualTo(409);
        assertThat(handler.conflict(new OptimisticTaskLockException("task-1", 1, 2))
                .getStatusCode().value()).isEqualTo(409);
        assertThat(handler.stateConflict(new IllegalStateException("confirmation has expired")))
                .satisfies(response -> {
                    assertThat(response.getStatusCode().value()).isEqualTo(409);
                    assertThat(response.getBody().code()).isEqualTo("AGENT_TASK_STATE_CONFLICT");
                });
    }

}
