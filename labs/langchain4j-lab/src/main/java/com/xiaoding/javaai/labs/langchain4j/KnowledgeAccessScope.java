package com.xiaoding.javaai.labs.langchain4j;

import java.util.List;

public record KnowledgeAccessScope(String tenantId, String subjectId, List<String> departments) {
    public KnowledgeAccessScope {
        if (tenantId == null || tenantId.isBlank() || subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("tenantId and subjectId must not be blank");
        }
        departments = List.copyOf(departments);
    }
}
