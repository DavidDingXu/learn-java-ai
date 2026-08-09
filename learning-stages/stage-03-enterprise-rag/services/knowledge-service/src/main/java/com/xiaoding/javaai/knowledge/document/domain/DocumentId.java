package com.xiaoding.javaai.knowledge.document.domain;

public record DocumentId(String value) {
    public DocumentId {
        value = requireText(value, "documentId");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
