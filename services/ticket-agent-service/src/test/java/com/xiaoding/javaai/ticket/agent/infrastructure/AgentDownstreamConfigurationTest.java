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
    void keeps_explicit_disabled_adapters_for_docker_free_local_startup() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AgentReadToolExecutor.class);
            assertThat(context).hasSingleBean(LegacyWriteToolExecutor.class);
            assertThat(context.getBean(AgentReadToolExecutor.class))
                    .isNotInstanceOf(HttpKnowledgeReadToolExecutor.class);
            assertThat(context.getBean(LegacyWriteToolExecutor.class))
                    .isNotInstanceOf(HttpLegacyWriteToolExecutor.class);
        });
    }

    @Test
    void wires_real_http_adapters_only_when_downstream_calls_are_enabled() {
        contextRunner
                .withBean(DownstreamAccessTokenProvider.class,
                        () -> (task, audience, scope) -> "delegated-token")
                .withPropertyValues(
                        "java-ai.agent.downstream-enabled=true",
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
