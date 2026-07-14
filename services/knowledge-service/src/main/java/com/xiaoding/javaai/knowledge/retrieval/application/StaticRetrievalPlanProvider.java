package com.xiaoding.javaai.knowledge.retrieval.application;

import com.xiaoding.javaai.knowledge.retrieval.application.port.RetrievalPlanProvider;

public final class StaticRetrievalPlanProvider implements RetrievalPlanProvider {

    private final RetrievalPlan plan;

    public StaticRetrievalPlanProvider(RetrievalPlan plan) {
        this.plan = plan;
    }

    @Override
    public RetrievalPlan planFor(RetrieveKnowledgeQuery query) {
        return plan;
    }
}
