package cn.dingxu.javaai.enterprise.rag;

import java.util.List;

public record RagAnswer(String content, List<Citation> citations, RagTrace trace) {
    public RagAnswer {
        content = content == null ? "" : content;
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
