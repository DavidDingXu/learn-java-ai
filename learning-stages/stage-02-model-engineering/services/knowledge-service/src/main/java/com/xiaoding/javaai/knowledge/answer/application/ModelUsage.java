package com.xiaoding.javaai.knowledge.answer.application;

public record ModelUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens
) {
}
