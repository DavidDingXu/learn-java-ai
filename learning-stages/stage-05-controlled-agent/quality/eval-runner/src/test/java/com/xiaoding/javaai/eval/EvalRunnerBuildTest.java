package com.xiaoding.javaai.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvalRunnerBuildTest {

    @TempDir
    Path tempDir;

    @Test
    void reportsPhaseFBuildVersion() {
        assertEquals("0.1.0", EvalRunner.version());
    }

    @Test
    void readsCredentialsFromAFileWithoutPuttingTheSecretInArguments() throws IOException {
        Path tokenFile = tempDir.resolve("bearer-token");
        Files.writeString(tokenFile, "short-lived-token\n");

        assertEquals(
                "short-lived-token",
                EvalRunner.requiredCredential(
                        Map.of("bearer-token-file", tokenFile.toString()),
                        "bearer-token"));
    }

    @Test
    void rejectsAmbiguousOrBlankCredentialSources() throws IOException {
        Path blankTokenFile = tempDir.resolve("blank-token");
        Files.writeString(blankTokenFile, "  \n");

        assertThrows(IllegalArgumentException.class, () -> EvalRunner.requiredCredential(
                Map.of("bearer-token", "inline", "bearer-token-file", blankTokenFile.toString()),
                "bearer-token"));
        assertThrows(IllegalArgumentException.class, () -> EvalRunner.requiredCredential(
                Map.of("bearer-token-file", blankTokenFile.toString()),
                "bearer-token"));
    }
}
