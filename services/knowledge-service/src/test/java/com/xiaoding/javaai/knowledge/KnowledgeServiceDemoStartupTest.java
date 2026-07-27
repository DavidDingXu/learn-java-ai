package com.xiaoding.javaai.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class KnowledgeServiceDemoStartupTest {

    @Autowired
    private Environment environment;

    @Test
    void defaultConfigurationStartsWithLocalDemoBoundaries() {
        assertThat(environment.acceptsProfiles(Profiles.of("demo"))).isTrue();
        assertThat(environment.getProperty(
                "java-ai.runtime.external-integrations-enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("java-ai.knowledge.context-source"))
                .isEqualTo("classpath");
        assertThat(environment.getProperty(
                "java-ai.knowledge.ingestion.enabled", Boolean.class)).isFalse();
    }
}
