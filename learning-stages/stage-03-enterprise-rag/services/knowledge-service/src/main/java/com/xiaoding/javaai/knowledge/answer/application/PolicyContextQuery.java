package com.xiaoding.javaai.knowledge.answer.application;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;

import java.time.Instant;

public record PolicyContextQuery(
        String question,
        KnowledgeAccessScope accessScope,
        Instant effectiveAt
) {
    public PolicyContextQuery(String question) {
        this(question, null, null);
    }
}
