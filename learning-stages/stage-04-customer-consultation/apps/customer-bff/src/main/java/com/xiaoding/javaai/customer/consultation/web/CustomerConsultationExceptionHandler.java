package com.xiaoding.javaai.customer.consultation.web;

import com.xiaoding.javaai.customer.consultation.application.ConsultationRateLimitExceededException;
import com.xiaoding.javaai.customer.consultation.application.ConversationAccessDeniedException;
import com.xiaoding.javaai.customer.downstream.DownstreamServiceException;
import com.xiaoding.javaai.customer.downstream.DownstreamTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class CustomerConsultationExceptionHandler {

    @ExceptionHandler(ConversationAccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(ConversationAccessDeniedException error) {
        return response(HttpStatus.FORBIDDEN, "CONVERSATION_ACCESS_DENIED", error.getMessage());
    }

    @ExceptionHandler(ConsultationRateLimitExceededException.class)
    ResponseEntity<ApiError> rateLimited(ConsultationRateLimitExceededException error) {
        return response(HttpStatus.TOO_MANY_REQUESTS, "CONSULTATION_RATE_LIMITED", error.getMessage());
    }

    @ExceptionHandler(DownstreamServiceException.class)
    ResponseEntity<ApiError> downstream(DownstreamServiceException error) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "DOWNSTREAM_SERVICE_FAILED",
                "A downstream service request failed"
        );
    }

    @ExceptionHandler(DownstreamTimeoutException.class)
    ResponseEntity<ApiError> downstreamTimeout(DownstreamTimeoutException error) {
        return response(
                HttpStatus.GATEWAY_TIMEOUT,
                "DOWNSTREAM_TIMEOUT",
                "A downstream service did not respond in time"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", error.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> conflict(IllegalStateException error) {
        return response(HttpStatus.CONFLICT, "CONSULTATION_STATE_CONFLICT", error.getMessage());
    }

    private static ResponseEntity<ApiError> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }

    record ApiError(String code, String message) {
    }
}
