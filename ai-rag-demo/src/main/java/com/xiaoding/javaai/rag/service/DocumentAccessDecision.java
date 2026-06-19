package com.xiaoding.javaai.rag.service;

public record DocumentAccessDecision(
        String documentId,
        String chunkId,
        boolean allowed,
        String reason
) {
    public DocumentAccessDecision {
        reason = reason == null || reason.isBlank() ? "unknown" : reason;
    }
}
