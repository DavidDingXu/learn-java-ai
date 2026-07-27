package com.xiaoding.javaai.knowledge.answer;

import com.xiaoding.javaai.knowledge.answer.application.GroundedPrompt;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ModelResilienceContractTest {

    private static final String ADAPTER_CLASS =
            "com.xiaoding.javaai.knowledge.answer.infrastructure.SpringAiKnowledgeAnswerModel";
    private static final String POLICY_NAME = "knowledgeAnswer";

    @Test
    void bindsAllResilienceAnnotationsToTheKnowledgeAnswerPolicy() throws ReflectiveOperationException {
        Method answer = Class.forName(ADAPTER_CLASS).getDeclaredMethod("answer", GroundedPrompt.class);

        Bulkhead bulkhead = answer.getAnnotation(Bulkhead.class);
        CircuitBreaker circuitBreaker = answer.getAnnotation(CircuitBreaker.class);
        TimeLimiter timeLimiter = answer.getAnnotation(TimeLimiter.class);
        Retry retry = answer.getAnnotation(Retry.class);

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.name()).isEqualTo(POLICY_NAME);
        assertThat(bulkhead.type()).isEqualTo(Bulkhead.Type.SEMAPHORE);
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.name()).isEqualTo(POLICY_NAME);
        assertThat(timeLimiter).isNotNull();
        assertThat(timeLimiter.name()).isEqualTo(POLICY_NAME);
        assertThat(retry).isNotNull();
        assertThat(retry.name()).isEqualTo(POLICY_NAME);
    }

    @Test
    void usesOneUseCaseSpecificPolicyWithoutStackingOpenAiSdkRetries() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.reactor.context-propagation")).isEqualTo("auto");
        assertThat(properties.getProperty("spring.ai.openai.max-retries")).isEqualTo("0");
        assertThat(properties).doesNotContainKey("spring.ai.retry.max-attempts");
        assertThat(properties.getProperty("resilience4j.retry.instances.knowledgeAnswer.max-attempts"))
                .isEqualTo("2");
        assertThat(properties.getProperty("resilience4j.timelimiter.instances.knowledgeAnswer.timeout-duration"))
                .isEqualTo("8s");
        assertThat(properties.getProperty("resilience4j.bulkhead.instances.knowledgeAnswer.max-concurrent-calls"))
                .isEqualTo("20");
    }
}
