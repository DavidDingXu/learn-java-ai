package cn.dingxu.javaai.rag;

import cn.dingxu.javaai.rag.service.DocumentChunk;
import cn.dingxu.javaai.rag.service.EmbeddingBatchService;
import cn.dingxu.javaai.rag.service.EmbeddingProvider;
import cn.dingxu.javaai.rag.service.EmbeddingResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingBatchServiceTest {

    @Test
    void embedsUniqueChunksInBatchesAndReusesCachedVectors() {
        CountingEmbeddingProvider provider = new CountingEmbeddingProvider(2);
        EmbeddingBatchService service = new EmbeddingBatchService(provider, 2);
        List<DocumentChunk> chunks = List.of(
                chunk("refund-policy-001-c1", "v1", "发货后退款需先核对物流状态。"),
                chunk("refund-policy-001-c2", "v1", "高金额退款必须转人工复核。"),
                chunk("refund-policy-001-c1", "v1", "发货后退款需先核对物流状态。")
        );

        List<EmbeddingResult> firstRun = service.embed(chunks);
        List<EmbeddingResult> secondRun = service.embed(chunks);

        assertThat(firstRun).hasSize(3);
        assertThat(firstRun.get(0).vector()).isEqualTo(firstRun.get(2).vector());
        assertThat(secondRun).extracting(EmbeddingResult::cacheHit)
                .containsOnly(true);
        assertThat(provider.calls()).hasSize(1);
        assertThat(provider.calls().get(0)).hasSize(2);
    }

    @Test
    void reEmbedsWhenChunkVersionChanges() {
        CountingEmbeddingProvider provider = new CountingEmbeddingProvider(10);
        EmbeddingBatchService service = new EmbeddingBatchService(provider, 10);

        service.embed(List.of(chunk("refund-policy-001-c1", "v1", "发货后退款需先核对物流状态。")));
        List<EmbeddingResult> updated = service.embed(List.of(
                chunk("refund-policy-001-c1", "v2", "发货后退款需先核对物流状态，并确认客户是否拒收。")
        ));

        assertThat(updated).hasSize(1);
        assertThat(updated.get(0).cacheHit()).isFalse();
        assertThat(provider.calls()).hasSize(2);
    }

    private DocumentChunk chunk(String chunkId, String version, String content) {
        return new DocumentChunk(
                "refund-policy-001",
                chunkId,
                "tenant-a",
                Set.of("support"),
                List.of("退款制度", "已发货退款"),
                "POLICY",
                version,
                content
        );
    }

    private static final class CountingEmbeddingProvider implements EmbeddingProvider {
        private final int dimension;
        private final List<List<String>> calls = new ArrayList<>();

        private CountingEmbeddingProvider(int dimension) {
            this.dimension = dimension;
        }

        @Override
        public List<List<Double>> embed(List<String> texts) {
            calls.add(List.copyOf(texts));
            return texts.stream()
                    .map(this::vector)
                    .toList();
        }

        private List<Double> vector(String text) {
            double length = text.length();
            double hash = Math.abs(text.hashCode() % 1000);
            List<Double> vector = new ArrayList<>();
            for (int i = 0; i < dimension; i++) {
                vector.add((length + hash + i) / 1000.0);
            }
            return vector;
        }

        private List<List<String>> calls() {
            return calls;
        }
    }
}
