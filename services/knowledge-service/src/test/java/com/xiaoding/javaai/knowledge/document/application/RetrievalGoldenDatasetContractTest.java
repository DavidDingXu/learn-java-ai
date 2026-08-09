package com.xiaoding.javaai.knowledge.document.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalGoldenDatasetContractTest {

    @Test
    void goldenSetReferencesChunkIdsProducedByTheVersionedFixture() throws Exception {
        Path fixture = Path.of("../../datasets/knowledge/refund-policy-chunking-v1.md")
                .toAbsolutePath().normalize();
        Path goldenSet = Path.of("../../datasets/retrieval/golden-set-v1.jsonl")
                .toAbsolutePath().normalize();
        Set<String> actualChunkIds = new LinkedHashSet<>(new PolicyDocumentChunker(1000)
                .chunk(new ChunkDocumentCommand(
                        new TenantId("tenant-a"),
                        new DocumentId("refund-policy"),
                        1,
                        "policy-chunk-v1",
                        Files.readString(fixture)
                ))
                .stream()
                .map(DocumentChunk::chunkId)
                .toList());

        Set<String> expectedChunkIds = new LinkedHashSet<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (String line : Files.readAllLines(goldenSet)) {
            if (line.isBlank()) continue;
            JsonNode parsed = objectMapper.readTree(line);
            parsed.path("expectedChunkIds").forEach(value -> expectedChunkIds.add(value.asText()));
        }

        assertThat(expectedChunkIds).isNotEmpty();
        assertThat(actualChunkIds).containsAll(expectedChunkIds);
    }
}
