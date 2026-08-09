package com.xiaoding.javaai.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=optional:file:../../config/application.yml",
                "java-ai.knowledge.mode=classpath"
        }
)
class KnowledgeServiceDefaultStartupTest {

    @Autowired
    private Environment environment;

    @Test
    void default_configuration_uses_real_chat_and_only_mocks_identity() {
        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("openai");
        assertThat(environment.getProperty("java-ai.knowledge.mode")).isEqualTo("classpath");
        assertThat(environment.getProperty("java-ai.security.mode")).isEqualTo("fixed");
    }
}
