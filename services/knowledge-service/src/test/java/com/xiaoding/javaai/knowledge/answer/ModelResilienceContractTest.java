package com.xiaoding.javaai.knowledge.answer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ModelResilienceContractTest {

    @Test
    void usesOneUseCaseSpecificPolicyWithoutStackingSpringAiRetries() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.reactor.context-propagation")).isEqualTo("auto");
        assertThat(properties.getProperty("spring.ai.retry.max-attempts")).isEqualTo("1");
        assertThat(properties.getProperty("resilience4j.retry.instances.knowledgeAnswer.max-attempts"))
                .isEqualTo("2");
        assertThat(properties.getProperty("resilience4j.timelimiter.instances.knowledgeAnswer.timeout-duration"))
                .isEqualTo("8s");
        assertThat(properties.getProperty("resilience4j.bulkhead.instances.knowledgeAnswer.max-concurrent-calls"))
                .isEqualTo("20");
    }
}
