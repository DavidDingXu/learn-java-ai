package com.xiaoding.javaai.customer.consultation.web;

import com.xiaoding.javaai.customer.consultation.application.CustomerAnswer;
import com.xiaoding.javaai.customer.consultation.domain.CitationView;

import java.util.List;

public record CustomerAnswerResponse(
        String conversationId,
        String attemptId,
        String retryOfAttemptId,
        String answer,
        List<CitationView> citations,
        boolean refused,
        String refusalReason,
        String traceId
) {
    static CustomerAnswerResponse from(CustomerAnswer answer) {
        return new CustomerAnswerResponse(
                answer.conversationId(), answer.attemptId(), answer.retryOfAttemptId(),
                answer.answer(), answer.citations(), answer.refused(),
                answer.refusalReason(), answer.traceId()
        );
    }
}
