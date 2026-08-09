package com.xiaoding.javaai.labs.langchain4j;

import java.util.List;

public record PolicyAnswer(String text, List<String> sourceIds) {
    public PolicyAnswer {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        sourceIds = List.copyOf(sourceIds);
    }

    public PolicyAnswer(String text) {
        this(text, List.of());
    }
}
