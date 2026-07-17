package com.xiaoding.javaai.knowledge.document.application;

public record DocumentAclGrant(DocumentAclSubjectType subjectType, String subjectId) {
    public DocumentAclGrant {
        if (subjectType == null) throw new IllegalArgumentException("subjectType must not be null");
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must not be blank");
        }
        subjectId = subjectId.strip();
    }
}
