package com.xiaoding.javaai.stages.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagStageProgressTest {

    @TempDir
    Path directory;

    @Test
    void savesPartialProgressSoRetriesDoNotRepeatCompletedWrites() {
        RagStageProgressStore store = new RagStageProgressStore(directory.resolve("state.json"));
        RagStageProgress progress = RagStageProgress.empty()
                .withAllowedUpload("refund-policy", 1, 2)
                .withBlockedUpload("finance-policy", 1, 3)
                .withAllowedIndexTask("task-a");

        store.save(progress);
        RagStageProgress restored = store.load();

        assertTrue(restored.uploaded());
        assertFalse(restored.published());
        assertEquals("task-a", restored.allowedIndexTaskId());
        assertEquals("refund-policy", restored.allowedDocumentId());
    }

    @Test
    void marksTheJourneyIndexedOnlyAfterBothPublishTasksExist() {
        RagStageProgress progress = RagStageProgress.empty()
                .withAllowedUpload("refund-policy", 1, 2)
                .withBlockedUpload("finance-policy", 1, 3)
                .withAllowedIndexTask("task-a")
                .withBlockedIndexTask("task-b")
                .markIndexed();

        assertTrue(progress.published());
        assertTrue(progress.indexed());
    }
}
