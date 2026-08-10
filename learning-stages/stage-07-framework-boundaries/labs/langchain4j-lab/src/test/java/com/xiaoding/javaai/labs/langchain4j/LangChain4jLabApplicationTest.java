package com.xiaoding.javaai.labs.langchain4j;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LangChain4jLabApplicationTest {

    @Test
    void loads_only_the_shared_model_settings_from_yaml() throws Exception {
        Path configFile = Files.createTempFile("langchain4j-lab-", ".yml");
        try {
            Files.writeString(configFile, """
                    lab:
                      mode: tool
                    spring:
                      ai:
                        openai:
                          api-key: test-key
                          base-url: https://example.test/v1
                          chat:
                            model: test-model
                    """);
            Properties config = new Properties();

            LangChain4jLabApplication.applyLocalOverride(config, configFile);

            assertNull(config.getProperty("lab.mode"));
            assertEquals("test-key", config.getProperty("lab.openai.api-key"));
            assertEquals("https://example.test/v1", config.getProperty("lab.openai.base-url"));
            assertEquals("test-model", config.getProperty("lab.openai.model"));
        } finally {
            Files.deleteIfExists(configFile);
        }
    }
}
