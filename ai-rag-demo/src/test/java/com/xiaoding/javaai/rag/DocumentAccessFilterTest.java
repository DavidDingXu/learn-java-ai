package com.xiaoding.javaai.rag;

import com.xiaoding.javaai.rag.service.DocumentAccessDecision;
import com.xiaoding.javaai.rag.service.DocumentAccessFilter;
import com.xiaoding.javaai.rag.service.DocumentChunk;
import com.xiaoding.javaai.rag.service.OperatorScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentAccessFilterTest {

    private final DocumentAccessFilter accessFilter = new DocumentAccessFilter();

    @Test
    void keepsOnlyChunksInTheSameTenantAndDepartment() {
        DocumentChunk supportChunk = chunk("support-c1", "tenant-a", Set.of("support"));
        DocumentChunk hrChunk = chunk("hr-c1", "tenant-a", Set.of("hr"));
        DocumentChunk otherTenantChunk = chunk("other-c1", "tenant-b", Set.of("support"));

        List<DocumentChunk> accessibleChunks = accessFilter.filterAccessible(
                List.of(supportChunk, hrChunk, otherTenantChunk),
                new OperatorScope("tenant-a", "support")
        );

        assertThat(accessibleChunks).containsExactly(supportChunk);
    }

    @Test
    void returnsDecisionReasonForTraceAndAudit() {
        DocumentAccessDecision allowed = accessFilter.decide(
                chunk("support-c1", "tenant-a", Set.of("support")),
                new OperatorScope("tenant-a", "support")
        );
        DocumentAccessDecision wrongDepartment = accessFilter.decide(
                chunk("hr-c1", "tenant-a", Set.of("hr")),
                new OperatorScope("tenant-a", "support")
        );
        DocumentAccessDecision wrongTenant = accessFilter.decide(
                chunk("other-c1", "tenant-b", Set.of("support")),
                new OperatorScope("tenant-a", "support")
        );

        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.reason()).isEqualTo("allowed");
        assertThat(wrongDepartment.allowed()).isFalse();
        assertThat(wrongDepartment.reason()).isEqualTo("department_mismatch");
        assertThat(wrongTenant.allowed()).isFalse();
        assertThat(wrongTenant.reason()).isEqualTo("tenant_mismatch");
    }

    @Test
    void treatsEmptyDepartmentAclAsDenied() {
        DocumentAccessDecision decision = accessFilter.decide(
                chunk("empty-acl", "tenant-a", Set.of()),
                new OperatorScope("tenant-a", "support")
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("department_mismatch");
    }

    private DocumentChunk chunk(String chunkId, String tenantId, Set<String> departments) {
        return new DocumentChunk(
                "refund-policy-001",
                chunkId,
                tenantId,
                departments,
                List.of("退款制度"),
                "POLICY",
                "v1",
                "退款制度。"
        );
    }
}
