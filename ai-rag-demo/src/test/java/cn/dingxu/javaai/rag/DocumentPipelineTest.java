package cn.dingxu.javaai.rag;

import cn.dingxu.javaai.rag.service.DocumentChunker;
import cn.dingxu.javaai.rag.service.DocumentMetadata;
import cn.dingxu.javaai.rag.service.DocumentParser;
import cn.dingxu.javaai.rag.service.ParsedDocument;
import cn.dingxu.javaai.rag.service.PolicyDocumentChunker;
import cn.dingxu.javaai.rag.service.SimpleDocumentParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentPipelineTest {

    @Test
    void parsesMarkdownAndKeepsDocumentMetadata() {
        DocumentParser parser = new SimpleDocumentParser();
        DocumentMetadata metadata = new DocumentMetadata(
                "refund-policy-001",
                "tenant-a",
                "退款制度",
                "POLICY",
                Set.of("support"),
                "v1"
        );

        ParsedDocument document = parser.parseMarkdown("""
                # 退款制度

                ## 已发货退款

                发货后退款需先核对物流状态。
                """, metadata);

        assertThat(document.metadata().documentId()).isEqualTo("refund-policy-001");
        assertThat(document.markdown()).contains("## 已发货退款");
        assertThat(document.plainText()).contains("发货后退款需先核对物流状态");
    }

    @Test
    void chunksMarkdownByHeadingAndCopiesMetadataToEachChunk() {
        DocumentParser parser = new SimpleDocumentParser();
        DocumentChunker chunker = new PolicyDocumentChunker(30);
        DocumentMetadata metadata = new DocumentMetadata(
                "refund-policy-001",
                "tenant-a",
                "退款制度",
                "POLICY",
                Set.of("support", "ops"),
                "v1"
        );
        ParsedDocument document = parser.parseMarkdown("""
                # 退款制度

                ## 已发货退款

                发货后退款需先核对物流状态，确认客户是否拒收、退回或已签收。

                ## 高金额退款

                高金额退款必须转人工复核，系统只能生成建议，不能直接执行退款。
                """, metadata);

        List<cn.dingxu.javaai.rag.service.DocumentChunk> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).documentId()).isEqualTo("refund-policy-001");
        assertThat(chunks.get(0).chunkId()).isEqualTo("refund-policy-001-c1");
        assertThat(chunks.get(0).headingPath()).containsExactly("退款制度", "已发货退款");
        assertThat(chunks.get(0).departments()).containsExactlyInAnyOrder("support", "ops");
        assertThat(chunks.get(1).headingPath()).containsExactly("退款制度", "高金额退款");
    }

    @Test
    void splitsLongSectionWithStableChunkIds() {
        DocumentChunker chunker = new PolicyDocumentChunker(20);
        DocumentMetadata metadata = new DocumentMetadata(
                "refund-policy-001",
                "tenant-a",
                "退款制度",
                "POLICY",
                Set.of("support"),
                "v1"
        );
        ParsedDocument document = new ParsedDocument(metadata, "# 退款制度\n\n## 已发货退款\n\n发货后退款需先核对物流状态，确认客户是否拒收、退回或已签收。", "发货后退款需先核对物流状态，确认客户是否拒收、退回或已签收。");

        List<cn.dingxu.javaai.rag.service.DocumentChunk> chunks = chunker.chunk(document);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting("chunkId")
                .contains("refund-policy-001-c1", "refund-policy-001-c2");
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.headingPath()).contains("退款制度", "已发货退款"));
    }
}
