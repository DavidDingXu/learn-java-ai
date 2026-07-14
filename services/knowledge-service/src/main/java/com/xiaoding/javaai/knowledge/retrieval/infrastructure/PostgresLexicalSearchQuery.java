package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record PostgresLexicalSearchQuery(String sql, List<Object> parameters) {

    public PostgresLexicalSearchQuery {
        parameters = List.copyOf(parameters);
    }

    public static PostgresLexicalSearchQuery create(
            String searchText,
            KnowledgeAccessScope scope,
            Instant effectiveAt,
            int topK
    ) {
        if (searchText == null || searchText.isBlank()) {
            throw new IllegalArgumentException("searchText must not be blank");
        }
        if (effectiveAt == null) throw new IllegalArgumentException("effectiveAt must not be null");
        if (topK < 1 || topK > 100) throw new IllegalArgumentException("topK must be between 1 and 100");

        StringBuilder acl = new StringBuilder("(acl.subject_type = 'USER' AND acl.subject_id = ?)");
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
                       similarity(dc.content, ?) AS score
                  FROM document_chunk dc
                  JOIN document_version dv
                    ON dv.tenant_id = dc.tenant_id
                   AND dv.document_id = dc.document_id
                   AND dv.version_number = dc.version_number
                  JOIN knowledge_document kd
                    ON kd.tenant_id = dc.tenant_id
                   AND kd.document_id = dc.document_id
                 WHERE dc.tenant_id = ?
                   AND kd.status = 'ACTIVE'
                   AND dv.status = 'PUBLISHED'
                   AND dv.effective_from <= ?
                   AND (dv.effective_until IS NULL OR dv.effective_until > ?)
                   AND (dc.content %% ? OR dc.content ILIKE '%%' || ? || '%%')
                   AND EXISTS (
                        SELECT 1
                          FROM document_acl acl
                         WHERE acl.tenant_id = dc.tenant_id
                           AND acl.document_id = dc.document_id
                           AND acl.permission = 'READ'
                           AND (%s)
                   )
                 ORDER BY score DESC, dc.chunk_id
                 LIMIT ?
                """.formatted(acl);

        String normalized = searchText.strip();
        List<Object> parameters = new ArrayList<>();
        parameters.add(normalized);
        parameters.add(scope.tenantId().value());
        parameters.add(effectiveAt);
        parameters.add(effectiveAt);
        parameters.add(normalized);
        parameters.add(normalized);
        parameters.add(scope.subjectId());
        parameters.addAll(scope.departmentIds());
        parameters.add(scope.tenantId().value());
        parameters.add(topK);
        return new PostgresLexicalSearchQuery(sql, parameters);
    }
}
