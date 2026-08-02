package com.xiaoding.javaai.knowledge.indexing.web;

import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskRunner;
import com.xiaoding.javaai.knowledge.security.KnowledgeAccessScopeProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/internal/v1/knowledge/index-tasks")
@ConditionalOnProperty(name = "java-ai.knowledge.mode", havingValue = "postgres-rag")
public final class IndexTaskController {

    private final IndexTaskRunner worker;
    private final KnowledgeAccessScopeProvider accessScopeProvider;

    public IndexTaskController(IndexTaskRunner worker, KnowledgeAccessScopeProvider accessScopeProvider) {
        this.worker = worker;
        this.accessScopeProvider = accessScopeProvider;
    }

    @PostMapping(path = "/run-once", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<IndexTaskRunResponse> runOnce(Authentication authentication) {
        var scope = accessScopeProvider.currentScope(authentication);
        return Mono.fromCallable(() -> worker.runOnce(scope.tenantId()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(IndexTaskRunResponse::from);
    }
}
