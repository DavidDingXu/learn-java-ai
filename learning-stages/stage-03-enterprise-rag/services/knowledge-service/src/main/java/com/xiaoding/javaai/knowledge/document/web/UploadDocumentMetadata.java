package com.xiaoding.javaai.knowledge.document.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UploadDocumentMetadata(
        @NotBlank @Size(max = 500) String title,
        @PositiveOrZero long expectedRevision
) {
}
