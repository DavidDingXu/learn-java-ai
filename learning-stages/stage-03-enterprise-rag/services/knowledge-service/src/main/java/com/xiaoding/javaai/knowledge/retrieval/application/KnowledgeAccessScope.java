package com.xiaoding.javaai.knowledge.retrieval.application;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.util.List;

public record KnowledgeAccessScope(
        TenantId tenantId,
        String subjectId,
        List<String> departmentIds
) {
    public KnowledgeAccessScope {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must not be blank");
        }
        departmentIds = List.copyOf(departmentIds);
        if (departmentIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("departmentIds must not contain blank values");
        }
    }
}
