package com.xiaoding.javaai.knowledge.retrieval.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record KnowledgeRetrievalEvaluationRequest(
        @NotBlank @Size(max = 2000) String question,
        @Min(1) @Max(100) int topK,
        RetrievalEvaluationMode mode
) {
    RetrievalEvaluationMode selectedMode() {
        return mode == null ? RetrievalEvaluationMode.DEFAULT : mode;
    }
}
