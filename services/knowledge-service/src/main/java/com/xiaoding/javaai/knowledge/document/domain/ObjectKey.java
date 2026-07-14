package com.xiaoding.javaai.knowledge.document.domain;

public record ObjectKey(String value) {
    public ObjectKey {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("objectKey must not be blank");
    }
}
