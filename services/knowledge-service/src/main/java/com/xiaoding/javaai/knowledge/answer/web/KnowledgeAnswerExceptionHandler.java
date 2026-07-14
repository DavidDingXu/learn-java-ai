package com.xiaoding.javaai.knowledge.answer.web;

import com.xiaoding.javaai.knowledge.answer.application.ModelNotConfiguredException;
import com.xiaoding.javaai.knowledge.answer.application.InvalidModelAnswerException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
final class KnowledgeAnswerExceptionHandler {

    @ExceptionHandler(ModelNotConfiguredException.class)
    ProblemDetail modelNotConfigured(ModelNotConfiguredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );
        problem.setTitle("Chat model is not configured");
        problem.setProperty("code", "MODEL_NOT_CONFIGURED");
        return problem;
    }

    @ExceptionHandler(InvalidModelAnswerException.class)
    ProblemDetail invalidModelAnswer(InvalidModelAnswerException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage()
        );
        problem.setTitle("Chat model returned an invalid business answer");
        problem.setProperty("code", "MODEL_OUTPUT_INVALID");
        return problem;
    }

    @ExceptionHandler(TimeoutException.class)
    ProblemDetail modelTimeout(TimeoutException exception) {
        return problem(HttpStatus.GATEWAY_TIMEOUT, "MODEL_TIMEOUT", "Chat model timed out");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    ProblemDetail modelCircuitOpen(CallNotPermittedException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "MODEL_CIRCUIT_OPEN", "Chat model is temporarily unavailable");
    }

    @ExceptionHandler(BulkheadFullException.class)
    ProblemDetail modelBusy(BulkheadFullException exception) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "MODEL_BUSY", "Chat model concurrency limit reached");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
