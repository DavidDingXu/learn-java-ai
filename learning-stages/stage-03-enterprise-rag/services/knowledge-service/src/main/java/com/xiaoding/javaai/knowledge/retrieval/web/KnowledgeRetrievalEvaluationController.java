package com.xiaoding.javaai.knowledge.retrieval.web;

import com.xiaoding.javaai.knowledge.retrieval.application.RetrieveKnowledgeQuery;
import com.xiaoding.javaai.knowledge.retrieval.application.HybridKnowledgeRetrievalService;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeRetrievalResult;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievalPlan;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeRetriever;
import com.xiaoding.javaai.knowledge.security.KnowledgeAccessScopeProvider;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Instant;

@RestController
@ConditionalOnProperty(name = "java-ai.knowledge.mode", havingValue = "postgres-rag")
@RequestMapping("/internal/v1/knowledge/retrieval/evaluations")
final class KnowledgeRetrievalEvaluationController {

    private final KnowledgeRetriever retriever;
    private final Clock clock;
    private final KnowledgeAccessScopeProvider accessScopeProvider;

    KnowledgeRetrievalEvaluationController(
            KnowledgeRetriever retriever,
            Clock clock,
            KnowledgeAccessScopeProvider accessScopeProvider
    ) {
        this.retriever = retriever;
        this.clock = clock;
        this.accessScopeProvider = accessScopeProvider;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<KnowledgeRetrievalEvaluationResponse> evaluate(
            @Valid @RequestBody KnowledgeRetrievalEvaluationRequest request,
            Authentication authentication
    ) {
        var query = new RetrieveKnowledgeQuery(
                request.question(), accessScopeProvider.currentScope(authentication), Instant.now(clock), request.topK()
        );
        return Mono.fromCallable(() -> KnowledgeRetrievalEvaluationResponse.from(retrieve(query, request)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private KnowledgeRetrievalResult retrieve(
            RetrieveKnowledgeQuery query,
            KnowledgeRetrievalEvaluationRequest request
    ) {
        if (request.selectedMode() == RetrievalEvaluationMode.DEFAULT) {
            return retriever.retrieve(query);
        }
        if (!(retriever instanceof HybridKnowledgeRetrievalService hybrid)) {
            if (request.selectedMode() == RetrievalEvaluationMode.VECTOR) {
                return retriever.retrieve(query);
            }
            throw new IllegalArgumentException(
                    "HYBRID evaluation requires java-ai.knowledge.retrieval.mode=hybrid"
            );
        }
        boolean lexicalSearch = request.selectedMode() == RetrievalEvaluationMode.HYBRID;
        return hybrid.retrieve(query, new RetrievalPlan(
                false,
                lexicalSearch,
                false,
                Math.max(request.topK(), 20)
        ));
    }
}
