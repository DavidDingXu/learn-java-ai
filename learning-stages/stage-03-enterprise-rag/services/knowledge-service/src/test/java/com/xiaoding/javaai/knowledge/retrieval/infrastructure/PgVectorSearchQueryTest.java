package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorSearchQueryTest {

    @Test
    void applies_tenant_acl_search_version_and_business_effective_time_before_topk_limit() {
        PgVectorSearchQuery query = PgVectorSearchQuery.create(
                new float[]{0.1f, 0.2f, 0.3f},
                "embedding-v1",
                new KnowledgeAccessScope(
                        new TenantId("tenant-a"),
                        "user-1",
                        List.of("support", "finance")
                ),
                Instant.parse("2026-07-12T10:00:00Z"),
                8
        );

        String sql = query.sql().toLowerCase();
        int limitIndex = sql.indexOf("limit ?");

        assertThat(limitIndex).isPositive();
        assertThat(sql.indexOf("dc.tenant_id = ?")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("dc.embedding_model = ?")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("kd.status = 'active'")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("join document_search_version dsv")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("dsv.version_number = dc.version_number")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("active_dv.status = 'published'")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("active_dv.effective_from <= ?")).isBetween(0, limitIndex);
        assertThat(sql).doesNotContain("join document_version dv");
        assertThat(sql.indexOf("acl.subject_type = 'user'")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("acl.subject_type = 'department'")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("order by dc.embedding <=> ?::vector")).isBetween(0, limitIndex);
        assertThat(query.parameters()).containsExactly(
                "[0.1,0.2,0.3]",
                "tenant-a",
                "embedding-v1",
                Instant.parse("2026-07-12T10:00:00Z"),
                Instant.parse("2026-07-12T10:00:00Z"),
                "user-1",
                "support",
                "finance",
                "tenant-a",
                "[0.1,0.2,0.3]",
                8
        );
    }

    @Test
    void omits_the_department_branch_when_the_identity_has_no_departments() {
        PgVectorSearchQuery query = PgVectorSearchQuery.create(
                new float[]{1.0f, 0.0f},
                "embedding-v1",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of()),
                Instant.parse("2026-07-12T10:00:00Z"),
                3
        );

        assertThat(query.sql()).doesNotContain("DEPARTMENT");
        assertThat(query.parameters()).containsExactly(
                "[1.0,0.0]",
                "tenant-a",
                "embedding-v1",
                Instant.parse("2026-07-12T10:00:00Z"),
                Instant.parse("2026-07-12T10:00:00Z"),
                "user-1",
                "tenant-a",
                "[1.0,0.0]",
                3
        );
    }
}
