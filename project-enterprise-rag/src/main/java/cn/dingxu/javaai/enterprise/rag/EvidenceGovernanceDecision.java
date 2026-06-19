package cn.dingxu.javaai.enterprise.rag;

import java.util.List;

public record EvidenceGovernanceDecision(
        EvidenceGovernanceStatus status,
        List<DocumentChunk> selectedEvidence,
        List<String> rejectedChunkIds,
        List<String> reasons
) {
    public EvidenceGovernanceDecision {
        status = status == null ? EvidenceGovernanceStatus.NO_EVIDENCE : status;
        selectedEvidence = selectedEvidence == null ? List.of() : List.copyOf(selectedEvidence);
        rejectedChunkIds = rejectedChunkIds == null ? List.of() : List.copyOf(rejectedChunkIds);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
