package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.PolicyContextQuery;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeRetrievalResult;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrieveKnowledgeQuery;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievedKnowledgeChunk;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeRetriever;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalPolicyContextSourceTest {

    @Test
    void carries_the_trusted_scope_into_retrieval_and_maps_chunks_to_citable_contexts() {
        RecordingRetriever retriever = new RecordingRetriever();
        RetrievalPolicyContextSource source = new RetrievalPolicyContextSource(retriever, 6);
        KnowledgeAccessScope scope = new KnowledgeAccessScope(
                new TenantId("tenant-a"), "customer-42", List.of("support")
        );
        Instant effectiveAt = Instant.parse("2026-07-13T03:00:00Z");

        var contexts = source.load(new PolicyContextQuery("退款多久到账？", scope, effectiveAt)).block();

        assertThat(retriever.query.question()).isEqualTo("退款多久到账？");
        assertThat(retriever.query.accessScope()).isSameAs(scope);
        assertThat(retriever.query.effectiveAt()).isEqualTo(effectiveAt);
        assertThat(retriever.query.topK()).isEqualTo(6);
        assertThat(contexts).singleElement().satisfies(context -> {
            assertThat(context.documentId()).isEqualTo("refund-policy");
            assertThat(context.version()).isEqualTo("2");
            assertThat(context.sectionId()).isEqualTo("chunk-10");
            assertThat(context.title()).isEqualTo("售后政策 / 退款 / 第十条");
            assertThat(context.content()).isEqualTo("退款审核通过后，一到五个工作日到账。");
        });
    }

    private static final class RecordingRetriever implements KnowledgeRetriever {
        private RetrieveKnowledgeQuery query;

        @Override
        public KnowledgeRetrievalResult retrieve(RetrieveKnowledgeQuery query) {
            this.query = query;
            return new KnowledgeRetrievalResult("embedding-v1", List.of(new RetrievedKnowledgeChunk(
                    "chunk-10",
                    new DocumentId("refund-policy"),
                    2,
                    List.of("售后政策", "退款"),
                    "第十条",
                    "退款审核通过后，一到五个工作日到账。",
                    0.92d
            )));
        }
    }
}
