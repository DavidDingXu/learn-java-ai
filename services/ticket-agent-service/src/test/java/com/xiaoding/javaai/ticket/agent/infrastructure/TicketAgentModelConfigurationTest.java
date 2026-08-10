package com.xiaoding.javaai.ticket.agent.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class TicketAgentModelConfigurationTest {

    @Test
    void disablesOpenAiSdkRetriesWithoutUsingTheRemovedGlobalRetryProperty() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(
                new ClassPathResource("application.yml"),
                new FileSystemResource(Path.of("..", "..", "config", "application-base.yml")));
        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.ai.openai.max-retries")).isEqualTo("0");
        assertThat(properties).doesNotContainKey("spring.ai.retry.max-attempts");
    }
}
