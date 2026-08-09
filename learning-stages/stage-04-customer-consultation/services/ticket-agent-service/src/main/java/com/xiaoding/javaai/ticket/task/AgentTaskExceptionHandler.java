package com.xiaoding.javaai.ticket.task;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class AgentTaskExceptionHandler {

    @ExceptionHandler({IdempotencyConflictException.class, OptimisticTaskLockException.class})
    ResponseEntity<ApiError> conflict(RuntimeException error) {
        String code = error instanceof OptimisticTaskLockException
                ? "AGENT_TASK_VERSION_CONFLICT"
                : "IDEMPOTENCY_CONFLICT";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(code, error.getMessage()));
    }

    @ExceptionHandler(AgentTaskNotFoundException.class)
    ResponseEntity<ApiError> notFound(AgentTaskNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("AGENT_TASK_NOT_FOUND", error.getMessage()));
    }

    @ExceptionHandler(AgentTaskAccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AgentTaskAccessDeniedException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("AGENT_TASK_ACCESS_DENIED", error.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> stateConflict(IllegalStateException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("AGENT_TASK_STATE_CONFLICT", error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_AGENT_TASK", error.getMessage()));
    }

    record ApiError(String code, String message) {
    }
}
