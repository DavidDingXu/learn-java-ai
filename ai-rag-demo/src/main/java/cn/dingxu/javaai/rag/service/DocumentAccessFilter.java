package cn.dingxu.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentAccessFilter {

    public List<DocumentChunk> filterAccessible(List<DocumentChunk> chunks, OperatorScope scope) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        return chunks.stream()
                .filter(chunk -> decide(chunk, scope).allowed())
                .toList();
    }

    public DocumentAccessDecision decide(DocumentChunk chunk, OperatorScope scope) {
        if (!chunk.tenantId().equals(scope.tenantId())) {
            return decision(chunk, false, "tenant_mismatch");
        }
        if (!chunk.departments().contains(scope.department())) {
            return decision(chunk, false, "department_mismatch");
        }
        return decision(chunk, true, "allowed");
    }

    private DocumentAccessDecision decision(DocumentChunk chunk, boolean allowed, String reason) {
        return new DocumentAccessDecision(chunk.documentId(), chunk.chunkId(), allowed, reason);
    }
}
