package com.xiaoding.javaai.knowledge.indexing.web;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskRunResult;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskRunner;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.test.StepVerifier;

import java.time.Instant;

class IndexTaskControllerTest {

    @Test
    void passes_the_verified_jwt_tenant_to_the_manual_worker_run() {
        RecordingRunner worker = new RecordingRunner();
        TenantId tenantId = new TenantId("tenant-a");

        StepVerifier.create(new IndexTaskController(worker).runOnce(jwt(tenantId)))
                .expectNext(new IndexTaskRunResponse("IDLE"))
                .verifyComplete();

        org.assertj.core.api.Assertions.assertThat(worker.tenantId).isEqualTo(tenantId);
    }

    @Test
    void returns_lost_lease_as_an_explicit_result_instead_of_an_http_error() {
        RecordingRunner worker = new RecordingRunner();
        worker.result = IndexTaskRunResult.LOST_LEASE;

        StepVerifier.create(new IndexTaskController(worker).runOnce(jwt(new TenantId("tenant-a"))))
                .expectNext(new IndexTaskRunResponse("LOST_LEASE"))
                .verifyComplete();
    }

    private static Jwt jwt(TenantId tenantId) {
        return Jwt.withTokenValue("verified-token")
                .header("alg", "HS256")
                .subject("operator-1")
                .issuedAt(Instant.parse("2026-07-13T04:00:00Z"))
                .expiresAt(Instant.parse("2026-07-13T05:00:00Z"))
                .claim("tenantId", tenantId.value())
                .build();
    }

    private static final class RecordingRunner implements IndexTaskRunner {
        private TenantId tenantId;
        private IndexTaskRunResult result = IndexTaskRunResult.IDLE;

        @Override
        public IndexTaskRunResult runOnce() {
            throw new AssertionError("manual controller must not invoke the global worker run");
        }

        @Override
        public IndexTaskRunResult runOnce(TenantId tenantId) {
            this.tenantId = tenantId;
            return result;
        }
    }
}
