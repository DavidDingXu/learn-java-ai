package com.xiaoding.javaai.knowledge.document.domain;

public final class DocumentRevisionConflictException extends RuntimeException {
    public DocumentRevisionConflictException(long expected, long actual) {
        super("expected revision " + expected + " but actual revision " + actual);
    }
}
