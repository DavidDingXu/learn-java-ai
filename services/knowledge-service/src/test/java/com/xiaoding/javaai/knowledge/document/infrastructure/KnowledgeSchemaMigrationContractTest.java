package com.xiaoding.javaai.knowledge.document.infrastructure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeSchemaMigrationContractTest {

    @Test
    void flyway_is_the_only_source_for_the_knowledge_platform_schema() throws IOException {
        String sql;
        try (var input = getClass().getResourceAsStream("/db/migration/V1__knowledge_platform.sql")) {
            assertThat(input).as("Flyway V1 knowledge migration").isNotNull();
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        assertThat(sql).contains("create extension if not exists vector");
        assertThat(sql).contains("create table knowledge_document");
        assertThat(sql).contains("create table document_version");
        assertThat(sql).contains("create table document_chunk");
        assertThat(sql).contains("create table document_acl");
        assertThat(sql).contains("create table index_task");
        assertThat(sql).contains("create table rag_feedback");
        assertThat(sql).contains("embedding vector(1536)");
        assertThat(sql).contains("unique (tenant_id, document_id, version_number)");
        assertThat(sql).contains("unique (tenant_id, document_id, content_hash)");
        assertThat(sql).contains("using hnsw (embedding vector_cosine_ops)");
    }

    @Test
    void publication_audit_columns_are_added_by_a_forward_only_migration() throws IOException {
        String sql;
        try (var input = getClass().getResourceAsStream("/db/migration/V3__document_publication_audit.sql")) {
            assertThat(input).as("Flyway V3 publication audit migration").isNotNull();
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        assertThat(sql).contains("alter table document_version");
        assertThat(sql).contains("add column published_by varchar(160)");
        assertThat(sql).contains("add column published_at timestamptz");
    }

    @Test
    void hybrid_search_migration_adds_the_trigram_extension_and_index() throws IOException {
        String sql;
        try (var input = getClass().getResourceAsStream("/db/migration/V2__knowledge_hybrid_search.sql")) {
            assertThat(input).as("Flyway V2 hybrid search migration").isNotNull();
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        assertThat(sql).contains("create extension if not exists pg_trgm");
        assertThat(sql).contains("using gin (content gin_trgm_ops)");
    }

    @Test
    void search_version_is_added_by_a_forward_only_migration() throws IOException {
        String sql;
        try (var input = getClass().getResourceAsStream("/db/migration/V4__document_search_version.sql")) {
            assertThat(input).as("Flyway V4 search version migration").isNotNull();
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        assertThat(sql).contains("create table document_search_version");
        assertThat(sql).contains("primary key (tenant_id, document_id)");
        assertThat(sql).contains("references document_version (tenant_id, document_id, version_number)");
        assertThat(sql).contains("where dv.status = 'published'");
        assertThat(sql).contains("from document_chunk dc");
    }
}
