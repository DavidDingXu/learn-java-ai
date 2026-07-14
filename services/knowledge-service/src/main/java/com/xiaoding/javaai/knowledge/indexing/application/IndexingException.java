package com.xiaoding.javaai.knowledge.indexing.application;

public final class IndexingException extends RuntimeException {

    private final String errorCode;

    public IndexingException(String errorCode, String message) {
        super(message);
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be blank");
        }
        this.errorCode = errorCode.strip();
    }

    public String errorCode() {
        return errorCode;
    }
}
