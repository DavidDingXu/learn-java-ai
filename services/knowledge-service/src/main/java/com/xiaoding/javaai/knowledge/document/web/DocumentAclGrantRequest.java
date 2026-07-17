package com.xiaoding.javaai.knowledge.document.web;

import com.xiaoding.javaai.knowledge.document.application.DocumentAclGrant;
import com.xiaoding.javaai.knowledge.document.application.DocumentAclSubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentAclGrantRequest(
        @NotNull DocumentAclSubjectType subjectType,
        @NotBlank @Size(max = 160) String subjectId
) {
    DocumentAclGrant toApplication() {
        return new DocumentAclGrant(subjectType, subjectId);
    }
}
