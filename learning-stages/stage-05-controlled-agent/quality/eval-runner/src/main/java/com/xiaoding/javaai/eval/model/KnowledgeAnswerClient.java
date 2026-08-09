package com.xiaoding.javaai.eval.model;

import java.net.URI;

@FunctionalInterface
public interface KnowledgeAnswerClient {

    KnowledgeAnswerSnapshot answer(URI baseUrl, String question);
}
