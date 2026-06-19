package com.xiaoding.javaai.rag;

import com.xiaoding.javaai.rag.service.DocumentChunk;
import com.xiaoding.javaai.rag.service.OperatorScope;
import com.xiaoding.javaai.rag.service.RagAnswer;
import com.xiaoding.javaai.rag.service.RagRetrievalService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RagRetrievalServiceTest {

    @Test
    void filtersDocumentsByTenantAndDepartmentBeforeAnswering() {
        RagRetrievalService service = new RagRetrievalService(List.of(
                new DocumentChunk("refund-001", "c1", "tenant-a", Set.of("support"), "发货后退款需先核对物流状态。"),
                new DocumentChunk("salary-001", "c2", "tenant-a", Set.of("hr"), "薪资制度仅 HR 可见。"),
                new DocumentChunk("refund-002", "c3", "tenant-b", Set.of("support"), "其他租户退款制度。")
        ));

        RagAnswer answer = service.answer("退款怎么处理", new OperatorScope("tenant-a", "support"));

        assertThat(answer.citations()).extracting("documentId").containsExactly("refund-001");
        assertThat(answer.content()).contains("发货后退款");
        assertThat(answer.content()).doesNotContain("薪资");
        assertThat(answer.content()).doesNotContain("其他租户");
    }

    @Test
    void returnsEmptyAnswerWhenNoAccessibleChunkMatches() {
        RagRetrievalService service = new RagRetrievalService(List.of(
                new DocumentChunk("salary-001", "c2", "tenant-a", Set.of("hr"), "薪资制度仅 HR 可见。")
        ));

        RagAnswer answer = service.answer("退款怎么处理", new OperatorScope("tenant-a", "support"));

        assertThat(answer.content()).isEqualTo("没有检索到当前用户可访问的依据。");
        assertThat(answer.citations()).isEmpty();
    }
}
