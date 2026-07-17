package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public record PgVectorSearchQuery(String sql, List<Object> parameters) {

    public PgVectorSearchQuery {
        parameters = List.copyOf(parameters);
    }

    public static PgVectorSearchQuery create(
            float[] embedding,
            String embeddingModel,
            KnowledgeAccessScope scope,
            Instant effectiveAt,
            int topK
    ) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("embedding must not be empty");
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        if (effectiveAt == null) throw new IllegalArgumentException("effectiveAt must not be null");
        if (topK < 1 || topK > 100) throw new IllegalArgumentException("topK must be between 1 and 100");

        StringBuilder acl = new StringBuilder("""
                (acl.subject_type = 'USER' AND acl.subject_id = ?)
                """);
        if (!scope.departmentIds().isEmpty()) {
            acl.append(" OR (acl.subject_type = 'DEPARTMENT' AND acl.subject_id IN (")
                    .append("?,".repeat(scope.departmentIds().size()));
            acl.setLength(acl.length() - 1);
            acl.append("))");
        }
        acl.append(" OR (acl.subject_type = 'TENANT' AND acl.subject_id = ?)");

        String sql = """
                SELECT dc.chunk_id,
                       dc.document_id,
                       dc.version_number,
                       dc.heading_path,
                       dc.clause,
                       dc.content,
                       1 - (dc.embedding <=> ?::vector) AS score
                  FROM document_chunk dc
                  JOIN document_search_version dsv
                    ON dsv.tenant_id = dc.tenant_id
                   AND dsv.document_id = dc.document_id
                   AND dsv.version_number = dc.version_number
                  JOIN knowledge_document kd
                    ON kd.tenant_id = dc.tenant_id
                   AND kd.document_id = dc.document_id
                 WHERE dc.tenant_id = ?
                   AND dc.embedding_model = ?
                   AND kd.status = 'ACTIVE'
                   AND dc.embedding IS NOT NULL
                   AND EXISTS (
                        SELECT 1
                          FROM document_version active_dv
                         WHERE active_dv.tenant_id = dc.tenant_id
                           AND active_dv.document_id = dc.document_id
                           AND active_dv.status = 'PUBLISHED'
                           AND active_dv.effective_from <= ?
                           AND (active_dv.effective_until IS NULL OR active_dv.effective_until > ?)
                   )
                   AND EXISTS (
                        SELECT 1
                          FROM document_acl acl
                         WHERE acl.tenant_id = dc.tenant_id
                           AND acl.document_id = dc.document_id
                           AND acl.permission = 'READ'
                           AND (%s)
                   )
                 ORDER BY dc.embedding <=> ?::vector
                 LIMIT ?
                """.formatted(acl);

        String vector = vectorLiteral(embedding);
        List<Object> parameters = new ArrayList<>();
        parameters.add(vector);
        parameters.add(scope.tenantId().value());
        parameters.add(embeddingModel.strip());
        parameters.add(effectiveAt);
        parameters.add(effectiveAt);
        parameters.add(scope.subjectId());
        parameters.addAll(scope.departmentIds());
        parameters.add(scope.tenantId().value());
        parameters.add(vector);
        parameters.add(topK);

        return new PgVectorSearchQuery(sql, parameters);
    }

    private static String vectorLiteral(float[] embedding) {
        StringJoiner values = new StringJoiner(",", "[", "]");
        for (float value : embedding) values.add(Float.toString(value));
        return values.toString();
    }
}
