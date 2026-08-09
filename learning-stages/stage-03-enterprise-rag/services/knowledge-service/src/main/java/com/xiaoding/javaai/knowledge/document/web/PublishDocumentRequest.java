package com.xiaoding.javaai.knowledge.document.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record PublishDocumentRequest(
        @PositiveOrZero long expectedRevision,
        @NotNull Instant effectiveFrom,
        Instant effectiveUntil,
        @NotEmpty @Size(max = 100) List<@Valid DocumentAclGrantRequest> acl
) {
}
