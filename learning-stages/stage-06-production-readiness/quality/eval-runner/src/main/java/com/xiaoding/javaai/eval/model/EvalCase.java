package com.xiaoding.javaai.eval.model;

import java.util.List;

public record EvalCase(
        String id,
        String question,
        List<String> expectedCitationSectionIds,
        boolean expectRefusal,
        boolean allowSafeRefusal,
        List<String> forbiddenPhrases
) {

    public EvalCase(
            String id,
            String question,
            List<String> expectedCitationSectionIds,
            boolean expectRefusal,
            List<String> forbiddenPhrases
    ) {
        this(id, question, expectedCitationSectionIds, expectRefusal, false, forbiddenPhrases);
    }

    public EvalCase {
        expectedCitationSectionIds = expectedCitationSectionIds == null
                ? List.of() : List.copyOf(expectedCitationSectionIds);
        forbiddenPhrases = forbiddenPhrases == null ? List.of() : List.copyOf(forbiddenPhrases);
    }
}
