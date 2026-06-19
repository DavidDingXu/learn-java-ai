package cn.dingxu.javaai.enterprise.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EnterpriseRagProjectTest {

    @Test
    void indexesUploadedDocumentIntoChunksAndRecordsIndexTask() {
        EnterpriseRagApplicationService service = EnterpriseRagApplicationService.seeded();
        PolicyDocumentUpload upload = new PolicyDocumentUpload(
                "refund-policy-2026",
                "tenant-a",
                Set.of("support", "ops"),
                DocumentType.POLICY,
                "退款制度\n发货后退款需先核对物流状态。\n高金额退款必须转人工复核。"
        );

        IndexTask task = service.uploadAndIndex(upload);

        assertThat(task.status()).isEqualTo(IndexTaskStatus.COMPLETED);
        assertThat(task.chunkCount()).isEqualTo(2);
        assertThat(service.documentRepository().get("refund-policy-2026").version()).isEqualTo(1);
        assertThat(service.chunkRepository().findByDocumentId("refund-policy-2026")).hasSize(2);
    }

    @Test
    void answersWithCitationsAfterTenantAndDepartmentFiltering() {
        EnterpriseRagApplicationService service = EnterpriseRagApplicationService.seeded();
        service.uploadAndIndex(new PolicyDocumentUpload(
                "refund-policy-2026",
                "tenant-a",
                Set.of("support"),
                DocumentType.POLICY,
                "退款制度\n发货后退款需先核对物流状态。\n高金额退款必须转人工复核。"
        ));
        service.uploadAndIndex(new PolicyDocumentUpload(
                "salary-policy-2026",
                "tenant-a",
                Set.of("hr"),
                DocumentType.POLICY,
                "薪资制度\n薪资调整仅 HR 和授权管理者可见。"
        ));

        RagAnswer answer = service.answer(
                "客户申请退款但订单已发货怎么办",
                new OperatorScope("tenant-a", "support")
        );

        assertThat(answer.content()).contains("发货后退款需先核对物流状态");
        assertThat(answer.citations()).extracting(Citation::documentId)
                .contains("refund-policy-2026")
                .doesNotContain("salary-policy-2026");
        assertThat(answer.trace().steps()).extracting(RagTraceStep::name)
                .containsExactly("query-rewrite", "access-filter", "hybrid-retrieve", "evidence-governance", "citation-compose");
    }

    @Test
    void incrementalReindexCreatesNewVersionAndRetiresOldChunks() {
        EnterpriseRagApplicationService service = EnterpriseRagApplicationService.seeded();
        service.uploadAndIndex(new PolicyDocumentUpload(
                "refund-policy-2026",
                "tenant-a",
                Set.of("support"),
                DocumentType.POLICY,
                "退款制度\n发货后退款需先核对物流状态。"
        ));

        IndexTask secondTask = service.uploadAndIndex(new PolicyDocumentUpload(
                "refund-policy-2026",
                "tenant-a",
                Set.of("support"),
                DocumentType.POLICY,
                "退款制度\n发货后退款需先核对物流状态。\n高金额退款必须转人工复核。"
        ));

        assertThat(secondTask.status()).isEqualTo(IndexTaskStatus.COMPLETED);
        assertThat(service.documentRepository().get("refund-policy-2026").version()).isEqualTo(2);
        assertThat(service.chunkRepository().findByDocumentId("refund-policy-2026"))
                .extracting(DocumentChunk::version)
                .containsOnly(2);
    }

    @Test
    void evaluationSeparatesRetrievalHitAndAnswerCitationQuality() {
        EnterpriseRagApplicationService service = EnterpriseRagApplicationService.seeded();
        service.uploadAndIndex(new PolicyDocumentUpload(
                "refund-policy-2026",
                "tenant-a",
                Set.of("support"),
                DocumentType.POLICY,
                "退款制度\n发货后退款需先核对物流状态。\n高金额退款必须转人工复核。"
        ));
        List<EvalCase> cases = List.of(new EvalCase(
                "E-001",
                "发货后退款怎么处理",
                new OperatorScope("tenant-a", "support"),
                "refund-policy-2026"
        ));

        EvalReport report = service.evaluate(cases);

        assertThat(report.caseCount()).isEqualTo(1);
        assertThat(report.retrievalHitRate()).isEqualByComparingTo("1.00");
        assertThat(report.citationHitRate()).isEqualByComparingTo("1.00");
    }

    @Test
    void refusesToAnswerWhenRetrievedEvidenceConflictsOnTheSameTopic() {
        EnterpriseRagApplicationService service = EnterpriseRagApplicationService.seeded();
        service.uploadAndIndex(new PolicyDocumentUpload(
                "refund-faq",
                "tenant-a",
                Set.of("support"),
                DocumentType.FAQ,
                "退款 FAQ\n#topic=refund-shipped priority=10 发货后退款可以直接退款。"
        ));
        service.uploadAndIndex(new PolicyDocumentUpload(
                "refund-policy-2026",
                "tenant-a",
                Set.of("support"),
                DocumentType.POLICY,
                "退款制度\n#topic=refund-shipped priority=20 发货后退款必须转人工复核，不能直接执行退款。"
        ));

        RagAnswer answer = service.answer(
                "发货后退款怎么处理",
                new OperatorScope("tenant-a", "support")
        );

        assertThat(answer.content()).contains("检索到相互冲突的制度依据");
        assertThat(answer.citations()).isEmpty();
        assertThat(answer.trace().steps()).extracting(RagTraceStep::name)
                .contains("evidence-governance");
        assertThat(answer.trace().steps()).extracting(RagTraceStep::detail)
                .anyMatch(detail -> detail.contains("status=CONFLICTED"));
    }
}
