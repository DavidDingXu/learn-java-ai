package com.xiaoding.javaai.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class TicketAgentDefaultStartupTest {

    @Autowired
    private Environment environment;

    @Test
    void default_configuration_runs_the_agent_with_local_state_and_real_knowledge_http() {
        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("openai");
        assertThat(environment.getProperty("java-ai.persistence.mode")).isEqualTo("memory");
        assertThat(environment.getProperty("java-ai.agent.knowledge-tool.mode")).isEqualTo("http");
        assertThat(environment.getProperty("java-ai.agent.write-tool.mode")).isEqualTo("memory");
    }
}
