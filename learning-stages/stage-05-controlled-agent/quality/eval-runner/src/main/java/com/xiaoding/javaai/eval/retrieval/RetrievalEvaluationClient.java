package com.xiaoding.javaai.eval.retrieval;

import java.net.URI;

@FunctionalInterface
public interface RetrievalEvaluationClient {

    RetrievalClientResult retrieve(URI baseUrl, String bearerToken, String question, int topK);
}
