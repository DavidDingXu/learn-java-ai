package com.xiaoding.javaai.knowledge.answer.application;

import java.util.List;

public record ModelStreamDecision(
        List<String> citedSectionIds,
        boolean refused,
        String refusalReason
) {

    public ModelStreamDecision {
        citedSectionIds = citedSectionIds == null ? List.of() : List.copyOf(citedSectionIds);
    }
}
