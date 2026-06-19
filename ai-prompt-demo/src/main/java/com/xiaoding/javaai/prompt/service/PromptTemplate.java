package com.xiaoding.javaai.prompt.service;

import java.time.Instant;

public record PromptTemplate(
        String code,
        String version,
        String content,
        Instant createdAt
) {
}
