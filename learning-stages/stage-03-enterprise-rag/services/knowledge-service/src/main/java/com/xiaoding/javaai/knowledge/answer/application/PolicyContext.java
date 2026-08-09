package com.xiaoding.javaai.knowledge.answer.application;

public record PolicyContext(
        String documentId,
        String version,
        String sectionId,
        String title,
        String content
) {
}
