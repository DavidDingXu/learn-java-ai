package com.xiaoding.javaai.ticket.agent.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketAgentLiveModelSmokeReportPathTest {

    @TempDir
    Path tempDir;

    @Test
    void preparesTheReportFileBeforeCallingTheProvider() throws IOException {
        Path reportPath = tempDir.resolve("reports/agent-live-model-smoke.md");

        Path prepared = TicketAgentLiveModelSmokeIT.prepareReportPath(reportPath);

        assertThat(prepared).isAbsolute();
        assertThat(Files.isRegularFile(prepared)).isTrue();
    }

    @Test
    void rejectsAReportPathThatCannotBeOpenedAsAFile() {
        assertThatThrownBy(() -> TicketAgentLiveModelSmokeIT.prepareReportPath(tempDir))
                .isInstanceOf(IOException.class);
    }
}
