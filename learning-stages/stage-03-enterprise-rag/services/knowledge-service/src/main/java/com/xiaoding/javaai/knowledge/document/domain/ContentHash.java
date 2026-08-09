package com.xiaoding.javaai.knowledge.document.domain;

public record ContentHash(String value) {
    public ContentHash {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("contentHash must not be blank");
    }
}
