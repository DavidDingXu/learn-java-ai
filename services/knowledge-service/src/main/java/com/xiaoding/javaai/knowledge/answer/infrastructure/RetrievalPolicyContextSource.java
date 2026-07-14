package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.PolicyContext;
import com.xiaoding.javaai.knowledge.answer.application.PolicyContextQuery;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrieveKnowledgeQuery;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievedKnowledgeChunk;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeRetriever;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

final class RetrievalPolicyContextSource implements PolicyContextSource {

    private final KnowledgeRetriever retriever;
    private final int topK;

    RetrievalPolicyContextSource(KnowledgeRetriever retriever, int topK) {
        if (topK < 1 || topK > 100) throw new IllegalArgumentException("topK must be between 1 and 100");
        this.retriever = retriever;
        this.topK = topK;
    }

    @Override
    public Mono<List<PolicyContext>> load(PolicyContextQuery query) {
        if (query.accessScope() == null || query.effectiveAt() == null) {
            return Mono.error(new IllegalArgumentException("trusted retrieval scope and effective time are required"));
        }
        return Mono.fromCallable(() -> retriever.retrieve(new RetrieveKnowledgeQuery(
                        query.question(), query.accessScope(), query.effectiveAt(), topK
                )).chunks().stream().map(RetrievalPolicyContextSource::toPolicyContext).toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static PolicyContext toPolicyContext(RetrievedKnowledgeChunk chunk) {
        List<String> titleParts = new ArrayList<>(chunk.headingPath());
        if (chunk.clause() != null && !chunk.clause().isBlank()) titleParts.add(chunk.clause());
        return new PolicyContext(
                chunk.documentId().value(),
                Integer.toString(chunk.documentVersion()),
                chunk.chunkId(),
                String.join(" / ", titleParts),
                chunk.content()
        );
    }
}
