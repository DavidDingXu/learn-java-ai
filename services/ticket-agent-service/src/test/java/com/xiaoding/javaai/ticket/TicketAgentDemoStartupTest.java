package com.xiaoding.javaai.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class TicketAgentDemoStartupTest {

    @Autowired
    private Environment environment;

    @Test
    void defaultConfigurationStartsWithLocalDemoBoundaries() {
        assertThat(environment.acceptsProfiles(Profiles.of("demo"))).isTrue();
        assertThat(environment.getProperty(
                "java-ai.runtime.external-integrations-enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("java-ai.persistence.mode")).isEqualTo("memory");
        assertThat(environment.getProperty(
                "java-ai.agent.downstream-enabled", Boolean.class)).isFalse();
    }
}
