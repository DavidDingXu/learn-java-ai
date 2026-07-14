package com.xiaoding.javaai.labs.langchain4j;

public record PolicyQuestion(String tenantId, String question) {
    public PolicyQuestion {
        if (tenantId == null || tenantId.isBlank() || question == null || question.isBlank()) {
            throw new IllegalArgumentException("tenantId and question must not be blank");
        }
    }
}
