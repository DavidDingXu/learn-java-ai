package com.xiaoding.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CitationAnswerComposer {

    private static final String NO_EVIDENCE = "没有检索到当前用户可访问的依据，不能生成处理建议。";

    public RagAnswer compose(String query,
                             List<RerankedRetrievalResult> selectedEvidence,
                             int maxEvidenceCount) {
        if (maxEvidenceCount <= 0 || selectedEvidence == null || selectedEvidence.isEmpty()) {
            return new RagAnswer(NO_EVIDENCE, List.of());
        }

        List<RerankedRetrievalResult> limitedEvidence = selectedEvidence.stream()
                .limit(maxEvidenceCount)
                .toList();

        if (limitedEvidence.isEmpty()) {
            return new RagAnswer(NO_EVIDENCE, List.of());
        }

        String answer = composeContent(limitedEvidence);
        List<Citation> citations = limitedEvidence.stream()
                .map(result -> new Citation(
                        result.chunk().documentId(),
                        result.chunk().chunkId(),
                        result.chunk().content()))
                .toList();

        return new RagAnswer(answer, citations);
    }

    private String composeContent(List<RerankedRetrievalResult> evidence) {
        StringBuilder builder = new StringBuilder();
        builder.append("根据当前可访问的制度依据：");
        for (int i = 0; i < evidence.size(); i++) {
            DocumentChunk chunk = evidence.get(i).chunk();
            builder.append("\n")
                    .append(i + 1)
                    .append(". ")
                    .append(chunk.content())
                    .append(" [")
                    .append(chunk.documentId())
                    .append("/")
                    .append(chunk.chunkId())
                    .append("]");
        }
        return builder.toString();
    }
}
