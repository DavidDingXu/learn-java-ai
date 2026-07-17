package com.xiaoding.javaai.knowledge.indexing.web;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/internal/v1/knowledge/index-tasks")
@ConditionalOnProperty(name = "java-ai.knowledge.ingestion.enabled", havingValue = "true")
public final class IndexTaskController {

    private final IndexTaskRunner worker;

    public IndexTaskController(IndexTaskRunner worker) {
        this.worker = worker;
    }

    @PostMapping(path = "/run-once", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<IndexTaskRunResponse> runOnce(@AuthenticationPrincipal Jwt jwt) {
        TenantId tenantId = new TenantId(jwt.getClaimAsString("tenantId"));
        return Mono.fromCallable(() -> worker.runOnce(tenantId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(IndexTaskRunResponse::from);
    }
}
