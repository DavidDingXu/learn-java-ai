package com.xiaoding.javaai.eval.retrieval;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetrievalEvalDatasetLoaderTest {

    @Test
    void loadsVersionedGoldenCasesAndRejectsDuplicateIds() throws Exception {
        Path valid = Files.createTempFile("retrieval-golden-set", ".jsonl");
        Files.writeString(valid, """
                {"datasetVersion":"retrieval-v1","id":"refund-arrival","question":"退款多久到账？","expectedChunkIds":["chunk-arrival"]}
                {"datasetVersion":"retrieval-v1","id":"refund-scope","question":"退款适用范围是什么？","expectedChunkIds":["chunk-scope"]}
                """);

        RetrievalEvalDataset dataset = new RetrievalEvalDatasetLoader().load(valid);

        assertEquals("retrieval-v1", dataset.version());
        assertEquals(2, dataset.cases().size());
        assertEquals("chunk-arrival", dataset.cases().getFirst().expectedChunkIds().iterator().next());

        Path duplicate = Files.createTempFile("retrieval-golden-set-duplicate", ".jsonl");
        Files.writeString(duplicate, """
                {"datasetVersion":"retrieval-v1","id":"same","question":"问题一","expectedChunkIds":["chunk-1"]}
                {"datasetVersion":"retrieval-v1","id":"same","question":"问题二","expectedChunkIds":["chunk-2"]}
                """);

        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalEvalDatasetLoader().load(duplicate));
    }
}
