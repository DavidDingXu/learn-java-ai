package com.xiaoding.javaai.knowledge.answer.web;

import com.xiaoding.javaai.knowledge.answer.application.Citation;
import com.xiaoding.javaai.knowledge.answer.application.ExecutionMode;
import com.xiaoding.javaai.knowledge.answer.application.KnowledgeAnswer;
import com.xiaoding.javaai.knowledge.answer.application.ModelUsage;

import java.util.List;

public record KnowledgeAnswerResponse(
        String answer,
        List<Citation> citations,
        boolean refused,
        String refusalReason,
        String model,
        ModelUsage usage,
        String finishReason,
        String traceId,
        ExecutionMode executionMode
) {

    static KnowledgeAnswerResponse from(KnowledgeAnswer answer) {
        return new KnowledgeAnswerResponse(
                answer.answer(),
                answer.citations(),
                answer.refused(),
                answer.refusalReason(),
                answer.model(),
                answer.usage(),
                answer.finishReason(),
                answer.traceId(),
                answer.executionMode()
        );
    }
}
