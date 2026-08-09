package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDocumentChunkerTest {

    @Test
    void keeps_heading_path_clause_and_version_metadata_in_each_chunk() {
        PolicyDocumentChunker chunker = new PolicyDocumentChunker(120);
        ChunkDocumentCommand command = command("""
                # 售后政策
                ## 退款
                第十条 退款审核通过后，原路退回。

                ### 到账时间
                银行卡通常需要一到五个工作日。
                """);

        List<DocumentChunk> chunks = chunker.chunk(command);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).headingPath()).containsExactly("售后政策", "退款");
        assertThat(chunks.get(0).clause()).isEqualTo("第十条");
        assertThat(chunks.get(0).documentId()).isEqualTo(new DocumentId("refund-policy"));
        assertThat(chunks.get(0).documentVersion()).isEqualTo(2);
        assertThat(chunks.get(0).chunkPolicyVersion()).isEqualTo("policy-chunker-v1");
        assertThat(chunks.get(1).headingPath()).containsExactly("售后政策", "退款", "到账时间");
    }

    @Test
    void generates_the_same_chunk_ids_for_the_same_normalized_content() {
        PolicyDocumentChunker chunker = new PolicyDocumentChunker(120);

        List<String> first = chunker.chunk(command("# 退款\r\n到账时间为一到五个工作日。"))
                .stream().map(DocumentChunk::chunkId).toList();
        List<String> second = chunker.chunk(command("# 退款\n到账时间为一到五个工作日。\n"))
                .stream().map(DocumentChunk::chunkId).toList();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void keeps_chunk_ids_distinct_when_identity_fields_contain_delimiters() {
        PolicyDocumentChunker chunker = new PolicyDocumentChunker(120);
        ChunkDocumentCommand first = new ChunkDocumentCommand(
                new TenantId("tenant|refund"),
                new DocumentId("policy"),
                2,
                "policy-chunker-v1",
                "# 退款\n到账时间为一到五个工作日。"
        );
        ChunkDocumentCommand second = new ChunkDocumentCommand(
                new TenantId("tenant"),
                new DocumentId("refund|policy"),
                2,
                "policy-chunker-v1",
                "# 退款\n到账时间为一到五个工作日。"
        );

        assertThat(chunker.chunk(first).getFirst().chunkId())
                .isNotEqualTo(chunker.chunk(second).getFirst().chunkId());
    }

    @Test
    void splits_oversized_content_without_exceeding_the_policy_limit() {
        PolicyDocumentChunker chunker = new PolicyDocumentChunker(30);

        List<DocumentChunk> chunks = chunker.chunk(command("""
                # 到账说明
                退款审核通过后会原路退回。银行卡处理通常需要一到五个工作日。超过五个工作日请联系人工客服。
                """));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.text().length()).isLessThanOrEqualTo(30);
            assertThat(chunk.headingPath()).containsExactly("到账说明");
        });
    }

    private static ChunkDocumentCommand command(String text) {
        return new ChunkDocumentCommand(
                new TenantId("tenant-a"),
                new DocumentId("refund-policy"),
                2,
                "policy-chunker-v1",
                text
        );
    }
}
