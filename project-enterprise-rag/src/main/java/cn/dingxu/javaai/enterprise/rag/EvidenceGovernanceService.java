package cn.dingxu.javaai.enterprise.rag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EvidenceGovernanceService {

    public EvidenceGovernanceDecision select(List<DocumentChunk> candidates, Instant now, int maxEvidenceCount) {
        if (maxEvidenceCount <= 0 || candidates == null || candidates.isEmpty()) {
            return new EvidenceGovernanceDecision(
                    EvidenceGovernanceStatus.NO_EVIDENCE,
                    List.of(),
                    chunkIds(candidates),
                    List.of("no_candidate")
            );
        }

        Instant decisionTime = now == null ? Instant.now() : now;
        List<String> rejectedChunkIds = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        List<DocumentChunk> active = new ArrayList<>();

        for (DocumentChunk chunk : candidates) {
            if (isExpired(chunk, decisionTime)) {
                rejectedChunkIds.add(chunk.chunkId());
                reasons.add("expired:" + chunk.chunkId());
                continue;
            }
            if (isNotYetEffective(chunk, decisionTime)) {
                rejectedChunkIds.add(chunk.chunkId());
                reasons.add("not_effective:" + chunk.chunkId());
                continue;
            }
            active.add(chunk);
        }

        if (active.isEmpty()) {
            return new EvidenceGovernanceDecision(
                    EvidenceGovernanceStatus.NO_EVIDENCE,
                    List.of(),
                    rejectedChunkIds,
                    reasons
            );
        }

        Map<String, List<DocumentChunk>> byTopic = new LinkedHashMap<>();
        for (DocumentChunk chunk : active) {
            byTopic.computeIfAbsent(chunk.topic(), ignored -> new ArrayList<>()).add(chunk);
        }

        for (Map.Entry<String, List<DocumentChunk>> entry : byTopic.entrySet()) {
            if (hasConflict(entry.getValue())) {
                rejectedChunkIds.addAll(entry.getValue().stream().map(DocumentChunk::chunkId).toList());
                reasons.add("conflict_topic:" + entry.getKey());
                return new EvidenceGovernanceDecision(
                        EvidenceGovernanceStatus.CONFLICTED,
                        List.of(),
                        rejectedChunkIds,
                        reasons
                );
            }
        }

        List<DocumentChunk> selected = active.stream()
                .sorted(Comparator.comparingInt(DocumentChunk::priority).reversed()
                        .thenComparingInt(DocumentChunk::version).reversed()
                        .thenComparing(DocumentChunk::documentId)
                        .thenComparing(DocumentChunk::ordinal))
                .limit(maxEvidenceCount)
                .toList();

        return new EvidenceGovernanceDecision(
                EvidenceGovernanceStatus.APPROVED,
                selected,
                rejectedChunkIds,
                reasons
        );
    }

    private boolean isExpired(DocumentChunk chunk, Instant now) {
        return chunk.effectiveTo() != null && !chunk.effectiveTo().isAfter(now);
    }

    private boolean isNotYetEffective(DocumentChunk chunk, Instant now) {
        return chunk.effectiveFrom() != null && chunk.effectiveFrom().isAfter(now);
    }

    private boolean hasConflict(List<DocumentChunk> chunks) {
        if (chunks.size() < 2) {
            return false;
        }
        boolean hasDirectAllowed = false;
        boolean hasManualReviewOrForbidden = false;
        for (DocumentChunk chunk : chunks) {
            String normalized = normalize(chunk.content());
            if (normalized.contains("可以直接") || normalized.contains("无需人工")) {
                hasDirectAllowed = true;
            }
            if (normalized.contains("必须转人工") || normalized.contains("不能直接") || normalized.contains("人工复核")) {
                hasManualReviewOrForbidden = true;
            }
        }
        return hasDirectAllowed && hasManualReviewOrForbidden;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private List<String> chunkIds(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        return chunks.stream().map(DocumentChunk::chunkId).toList();
    }
}
