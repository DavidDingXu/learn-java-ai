package cn.dingxu.javaai.eval.service;

import java.util.List;

public record RagEvalObservation(
        String caseId,
        List<String> retrievedChunkIds,
        List<String> citedChunkIds,
        boolean noEvidenceReturned,
        String answer
) {
    public RagEvalObservation {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        retrievedChunkIds = retrievedChunkIds == null ? List.of() : List.copyOf(retrievedChunkIds);
        citedChunkIds = citedChunkIds == null ? List.of() : List.copyOf(citedChunkIds);
        answer = answer == null ? "" : answer;
    }
}
