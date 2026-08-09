package com.xiaoding.javaai.knowledge.answer.application;

import java.util.List;

public record ModelAnswerDraft(
        String answer,
        List<String> citedSectionIds,
        boolean refused,
        String refusalReason,
        String model,
        ModelUsage usage,
        String finishReason
) {

    public ModelAnswerDraft {
        citedSectionIds = citedSectionIds == null ? List.of() : List.copyOf(citedSectionIds);
    }
}
