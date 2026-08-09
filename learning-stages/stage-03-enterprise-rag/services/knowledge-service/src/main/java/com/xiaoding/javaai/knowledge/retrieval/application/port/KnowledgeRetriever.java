package com.xiaoding.javaai.knowledge.retrieval.application.port;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeRetrievalResult;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrieveKnowledgeQuery;

public interface KnowledgeRetriever {
    KnowledgeRetrievalResult retrieve(RetrieveKnowledgeQuery query);
}
