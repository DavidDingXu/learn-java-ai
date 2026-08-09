package com.xiaoding.javaai.knowledge.retrieval.application.port;

import com.xiaoding.javaai.knowledge.retrieval.application.RetrievalPlan;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrieveKnowledgeQuery;

public interface RetrievalPlanProvider {
    RetrievalPlan planFor(RetrieveKnowledgeQuery query);
}
