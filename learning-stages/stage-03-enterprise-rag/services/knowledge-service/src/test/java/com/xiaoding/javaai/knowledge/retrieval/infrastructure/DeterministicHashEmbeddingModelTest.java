package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicHashEmbeddingModelTest {

    @Test
    void creates_stable_normalized_vectors_for_local_development() {
        DeterministicHashEmbeddingModel model = new DeterministicHashEmbeddingModel(32);

        var first = model.embed("退款多久能到账？");
        var second = model.embed("退款多久能到账？");
        var different = model.embed("如何修改收货地址？");

        assertThat(first.model()).isEqualTo("deterministic-hash-v1-32");
        assertThat(first.vector()).hasSize(32).containsExactly(second.vector());
        assertThat(first.vector()).isNotEqualTo(different.vector());

        double norm = 0;
        for (float value : first.vector()) norm += value * value;
        assertThat(Math.sqrt(norm)).isCloseTo(1.0d, within(0.0001d));
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
