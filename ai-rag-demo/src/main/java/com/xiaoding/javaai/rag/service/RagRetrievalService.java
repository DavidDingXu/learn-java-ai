package com.xiaoding.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class RagRetrievalService {

    private static final String NO_EVIDENCE = "没有检索到当前用户可访问的依据。";

    private final List<DocumentChunk> chunks;
    private final DocumentAccessFilter accessFilter;

    public RagRetrievalService() {
        this(List.of(
                new DocumentChunk("refund-policy-001", "c1", "tenant-a", Set.of("support"),
                        "发货后退款需先核对物流状态，确认客户是否拒收、退回或已签收。"),
                new DocumentChunk("refund-policy-002", "c2", "tenant-a", Set.of("support", "ops"),
                        "高金额退款必须转人工复核，系统只能生成建议，不能直接执行退款。"),
                new DocumentChunk("hr-policy-001", "c3", "tenant-a", Set.of("hr"),
                        "薪资制度仅 HR 部门可见。")
        ));
    }

    public RagRetrievalService(List<DocumentChunk> chunks) {
        this(chunks, new DocumentAccessFilter());
    }

    public RagRetrievalService(List<DocumentChunk> chunks, DocumentAccessFilter accessFilter) {
        this.chunks = chunks == null ? List.of() : List.copyOf(chunks);
        this.accessFilter = accessFilter == null ? new DocumentAccessFilter() : accessFilter;
    }

    public RagAnswer answer(String query, OperatorScope scope) {
        List<DocumentChunk> matchedChunks = chunks.stream()
                .filter(chunk -> accessFilter.decide(chunk, scope).allowed())
                .filter(chunk -> matches(query, chunk.content()))
                .sorted(Comparator.comparing(DocumentChunk::documentId).thenComparing(DocumentChunk::chunkId))
                .limit(3)
                .toList();

        if (matchedChunks.isEmpty()) {
            return new RagAnswer(NO_EVIDENCE, List.of());
        }

        String evidence = matchedChunks.stream()
                .map(DocumentChunk::content)
                .reduce((left, right) -> left + "\n" + right)
                .orElse(NO_EVIDENCE);

        List<Citation> citations = matchedChunks.stream()
                .map(chunk -> new Citation(chunk.documentId(), chunk.chunkId(), chunk.content()))
                .toList();

        return new RagAnswer(evidence, citations);
    }

    private boolean matches(String query, String content) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String normalized = query.replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length(); i++) {
            String token = normalized.substring(i, i + 1);
            if (!isStopToken(token) && content.contains(token)) {
                return true;
            }
        }
        return content.contains(query);
    }

    private boolean isStopToken(String token) {
        return Set.of("怎", "么", "处", "理", "的", "了", "吗", "？", "?").contains(token);
    }
}
