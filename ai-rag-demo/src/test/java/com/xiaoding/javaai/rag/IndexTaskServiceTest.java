package com.xiaoding.javaai.rag;

import com.xiaoding.javaai.rag.service.DocumentChunk;
import com.xiaoding.javaai.rag.service.DocumentMetadata;
import com.xiaoding.javaai.rag.service.DocumentParser;
import com.xiaoding.javaai.rag.service.EmbeddingProvider;
import com.xiaoding.javaai.rag.service.InMemoryVectorIndex;
import com.xiaoding.javaai.rag.service.IndexTaskResult;
import com.xiaoding.javaai.rag.service.IndexTaskService;
import com.xiaoding.javaai.rag.service.IndexTaskStatus;
import com.xiaoding.javaai.rag.service.OperatorScope;
import com.xiaoding.javaai.rag.service.ParsedDocument;
import com.xiaoding.javaai.rag.service.PolicyDocumentChunker;
import com.xiaoding.javaai.rag.service.VectorSearchResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IndexTaskServiceTest {

    @Test
    void indexesParsedDocumentIntoVectorIndexAndReturnsCompletedTask() {
        CountingEmbeddingProvider provider = new CountingEmbeddingProvider();
        InMemoryVectorIndex vectorIndex = new InMemoryVectorIndex();
        IndexTaskService service = new IndexTaskService(
                parser("refund-policy-001", "v1", "# 退款制度\n发货后退款需先核对物流状态。"),
                new PolicyDocumentChunker(),
                provider,
                vectorIndex
        );

        IndexTaskResult result = service.rebuild("refund-policy-001", "raw");

        assertThat(result.status()).isEqualTo(IndexTaskStatus.COMPLETED);
        assertThat(result.documentId()).isEqualTo("refund-policy-001");
        assertThat(result.version()).isEqualTo("v1");
        assertThat(result.chunkCount()).isGreaterThan(0);
        assertThat(result.errorMessage()).isBlank();
        assertThat(provider.calls()).hasSize(1);

        List<VectorSearchResult> searchResults = vectorIndex.search(
                List.of(1.0, 0.0),
                new OperatorScope("tenant-a", "support"),
                5
        );
        assertThat(searchResults).isNotEmpty();
        assertThat(searchResults.get(0).chunk().documentId()).isEqualTo("refund-policy-001");
    }

    @Test
    void reIndexesDocumentWhenVersionChanges() {
        CountingEmbeddingProvider provider = new CountingEmbeddingProvider();
        InMemoryVectorIndex vectorIndex = new InMemoryVectorIndex();
        IndexTaskService firstService = new IndexTaskService(
                parser("refund-policy-001", "v1", "# 退款制度\n发货后退款需先核对物流状态。"),
                new PolicyDocumentChunker(),
                provider,
                vectorIndex
        );
        IndexTaskService secondService = new IndexTaskService(
                parser("refund-policy-001", "v2", "# 退款制度\n发货后退款需先核对物流状态，并确认客户是否拒收。"),
                new PolicyDocumentChunker(),
                provider,
                vectorIndex
        );

        IndexTaskResult first = firstService.rebuild("refund-policy-001", "raw-v1");
        IndexTaskResult second = secondService.rebuild("refund-policy-001", "raw-v2");

        assertThat(first.status()).isEqualTo(IndexTaskStatus.COMPLETED);
        assertThat(second.status()).isEqualTo(IndexTaskStatus.COMPLETED);
        assertThat(second.version()).isEqualTo("v2");
        assertThat(provider.calls()).hasSize(2);
    }

    @Test
    void marksTaskFailedWhenDocumentProducesNoChunks() {
        CountingEmbeddingProvider provider = new CountingEmbeddingProvider();
        IndexTaskService service = new IndexTaskService(
                parser("empty-policy-001", "v1", " "),
                new PolicyDocumentChunker(),
                provider,
                new InMemoryVectorIndex()
        );

        IndexTaskResult result = service.rebuild("empty-policy-001", "empty");

        assertThat(result.status()).isEqualTo(IndexTaskStatus.FAILED);
        assertThat(result.chunkCount()).isZero();
        assertThat(result.errorMessage()).isEqualTo("document produced no chunks");
        assertThat(provider.calls()).isEmpty();
    }

    private DocumentParser parser(String documentId, String version, String markdown) {
        return new DocumentParser() {
            @Override
            public ParsedDocument parseMarkdown(String ignored, DocumentMetadata metadata) {
                return parsedDocument(documentId, version, markdown);
            }

            @Override
            public ParsedDocument parsePlainText(String ignored, DocumentMetadata metadata) {
                return parsedDocument(documentId, version, markdown);
            }
        };
    }

    private ParsedDocument parsedDocument(String documentId, String version, String markdown) {
        return new ParsedDocument(
            new DocumentMetadata(
                    documentId,
                    "tenant-a",
                    "退款制度",
                    "POLICY",
                    Set.of("support"),
                    version
            ),
            markdown,
            markdown.replace("#", "")
        );
    }

    private static final class CountingEmbeddingProvider implements EmbeddingProvider {
        private final List<List<String>> calls = new ArrayList<>();

        @Override
        public List<List<Double>> embed(List<String> texts) {
            calls.add(List.copyOf(texts));
            return texts.stream()
                    .map(this::vector)
                    .toList();
        }

        private List<Double> vector(String text) {
            return List.of(1.0, Math.max(1, text.length()) / 100.0);
        }

        private List<List<String>> calls() {
            return calls;
        }
    }
}
