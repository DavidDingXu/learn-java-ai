package com.xiaoding.javaai.knowledge.answer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveModelSmokeReportPathTest {

    @TempDir
    Path tempDir;

    @Test
    void preparesTheReportFileBeforeCallingTheProvider() throws IOException {
        Path reportPath = tempDir.resolve("reports/live-model-smoke.md");

        Path prepared = LiveModelSmokeIT.prepareReportPath(reportPath);

        assertThat(prepared).isAbsolute();
        assertThat(Files.isRegularFile(prepared)).isTrue();
    }

    @Test
    void rejectsAReportPathThatCannotBeOpenedAsAFile() {
        assertThatThrownBy(() -> LiveModelSmokeIT.prepareReportPath(tempDir))
                .isInstanceOf(IOException.class);
    }
}
