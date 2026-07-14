package com.xiaoding.javaai.labs.langchain4j;

import java.util.List;

public interface KnowledgeSearchPort {
    List<KnowledgeSnippet> search(KnowledgeAccessScope scope, String query, int topK);
}
