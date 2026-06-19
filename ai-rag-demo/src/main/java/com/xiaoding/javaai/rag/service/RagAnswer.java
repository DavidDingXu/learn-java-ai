package com.xiaoding.javaai.rag.service;

import java.util.List;

public record RagAnswer(String content, List<Citation> citations) {
    public RagAnswer {
        content = content == null ? "" : content;
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
