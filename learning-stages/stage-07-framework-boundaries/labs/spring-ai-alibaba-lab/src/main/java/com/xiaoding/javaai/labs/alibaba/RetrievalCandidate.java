package com.xiaoding.javaai.labs.alibaba;

public record RetrievalCandidate(String id, String text) {

    public RetrievalCandidate {
        id = requireText(id, "id");
        text = requireText(text, "text");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
