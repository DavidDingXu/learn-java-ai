package com.xiaoding.javaai.knowledge.answer.application;

public record Citation(
        String documentId,
        String version,
        String sectionId,
        String title
) {
}
