package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentReadToolExecutor;
import com.xiaoding.javaai.ticket.agent.application.DownstreamAccessTokenProvider;
import com.xiaoding.javaai.ticket.agent.application.LegacyWriteToolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDownstreamConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AgentDownstreamConfiguration.class)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void uses_http_for_knowledge_and_an_in_memory_write_tool_by_default() {
        contextRunner
                .withPropertyValues("java-ai.agent.knowledge-base-url=http://knowledge.test")
                .run(context -> {
            assertThat(context).hasSingleBean(AgentReadToolExecutor.class);
            assertThat(context).hasSingleBean(LegacyWriteToolExecutor.class);
            assertThat(context.getBean(AgentReadToolExecutor.class))
                    .isInstanceOf(HttpKnowledgeReadToolExecutor.class);
            assertThat(context.getBean(LegacyWriteToolExecutor.class))
                    .isInstanceOf(InMemoryLegacyWriteToolExecutor.class);
            assertThat(context).hasSingleBean(DownstreamAccessTokenProvider.class);
        });
    }

    @Test
    void can_replace_the_local_write_tool_with_the_http_adapter() {
        contextRunner
                .withBean(DownstreamAccessTokenProvider.class,
                        () -> (task, audience, scope) -> "delegated-token")
                .withPropertyValues(
                        "java-ai.agent.write-tool.mode=http",
                        "java-ai.agent.downstream-token-mode=external",
                        "java-ai.agent.knowledge-base-url=http://knowledge.test",
                        "java-ai.agent.legacy-tool-base-url=http://legacy.test",
                        "java-ai.agent.downstream-timeout=2s")
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentReadToolExecutor.class);
                    assertThat(context).hasSingleBean(LegacyWriteToolExecutor.class);
                    assertThat(context.getBean(AgentReadToolExecutor.class))
                            .isInstanceOf(HttpKnowledgeReadToolExecutor.class);
                    assertThat(context.getBean(LegacyWriteToolExecutor.class))
                            .isInstanceOf(HttpLegacyWriteToolExecutor.class);
                });
    }
}
