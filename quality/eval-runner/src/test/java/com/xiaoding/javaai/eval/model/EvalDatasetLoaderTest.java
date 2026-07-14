package com.xiaoding.javaai.eval.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalDatasetLoaderTest {

    @Test
    void rejectsDuplicateCaseIds() throws Exception {
        Path dataset = Files.createTempFile("duplicate-eval", ".jsonl");
        Files.writeString(dataset, """
                {"datasetVersion":"v1","id":"refund-arrival","question":"问题一","expectedCitationSectionIds":["arrival-time"],"expectRefusal":false,"forbiddenPhrases":[]}
                {"datasetVersion":"v1","id":"refund-arrival","question":"问题二","expectedCitationSectionIds":[],"expectRefusal":true,"forbiddenPhrases":[]}
                """);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new EvalDatasetLoader().load(dataset)
        );
        assertTrue(error.getMessage().contains("duplicate case id"));
    }

    @Test
    void loadsOptionalSafeRefusalPolicy() throws Exception {
        Path dataset = Files.createTempFile("safe-refusal-eval", ".jsonl");
        Files.writeString(dataset, """
                {"datasetVersion":"v2","id":"prompt-injection","question":"问题","expectedCitationSectionIds":["arrival-time"],"expectRefusal":false,"allowSafeRefusal":true,"forbiddenPhrases":["你是企业客户服务知识助手"]}
                """);

        EvalCase loaded = new EvalDatasetLoader().load(dataset).cases().getFirst();

        assertEquals(false, loaded.expectRefusal());
        assertEquals(true, loaded.allowSafeRefusal());
    }
}
