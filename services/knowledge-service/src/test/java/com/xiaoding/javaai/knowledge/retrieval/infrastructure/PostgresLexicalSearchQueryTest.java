package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresLexicalSearchQueryTest {

    @Test
    void applies_document_acl_search_version_and_business_effective_time_before_the_lexical_topk() {
        Instant now = Instant.parse("2026-07-13T06:00:00Z");
        PostgresLexicalSearchQuery query = PostgresLexicalSearchQuery.create(
                "退款到账时效",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of("support")),
                now,
                12
        );

        String sql = query.sql().toLowerCase();
        int limitIndex = sql.indexOf("limit ?");
        assertThat(sql).contains("similarity(dc.content, ?)");
        assertThat(sql).contains("dc.content % ?");
        assertThat(sql.indexOf("kd.status = 'active'")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("join document_search_version dsv")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("dsv.version_number = dc.version_number")).isBetween(0, limitIndex);
        assertThat(sql.indexOf("active_dv.status = 'published'")).isBetween(0, limitIndex);
        assertThat(sql).doesNotContain("join document_version dv");
        assertThat(sql.indexOf("acl.subject_type = 'department'")).isBetween(0, limitIndex);
        assertThat(query.parameters()).containsExactly(
                "退款到账时效",
                "tenant-a",
                now,
                now,
                "退款到账时效",
                "退款到账时效",
                "user-1",
                "support",
                "tenant-a",
                12
        );
    }
}
