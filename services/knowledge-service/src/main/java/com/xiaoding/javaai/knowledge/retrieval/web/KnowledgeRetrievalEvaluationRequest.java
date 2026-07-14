package com.xiaoding.javaai.knowledge.retrieval.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

record KnowledgeRetrievalEvaluationRequest(
        @NotBlank String question,
        @Min(1) @Max(100) int topK
) {
}
