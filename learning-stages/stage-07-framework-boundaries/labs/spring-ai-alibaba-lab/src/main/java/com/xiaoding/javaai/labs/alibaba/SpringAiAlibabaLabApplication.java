package com.xiaoding.javaai.labs.alibaba;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SpringAiAlibabaLabApplication {

    private SpringAiAlibabaLabApplication() {
    }

    public static void main(String[] args) {
        Properties config = loadConfig();
        runProvider(config);
    }

    static void runProvider(Properties config) {
        String apiKey = configuredSecret(config, "lab.dashscope.api-key");
        String model = required(config, "lab.dashscope.chat-model");
        DashScopeApi api = DashScopeApi.builder()
                .baseUrl(required(config, "lab.dashscope.base-url"))
                .apiKey(apiKey)
                .build();
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(DashScopeChatOptions.builder().model(model).build())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
        ProviderAnswer answer = new DashScopeProviderAdapter(chatModel, model, 0.1, 500)
                .answer("你是企业政策助手，没有依据时拒答。", "退款审核通过后通常多久到账？");
        System.out.printf("model=%s usage=%s latencyMs=%d responseId=%s answer=%s%n",
                answer.model(), answer.usage(), answer.latency().toMillis(),
                answer.providerMetadata().getOrDefault("responseId", "unavailable"), answer.text());
    }

    static Properties loadConfig() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();
        if (properties == null) throw new IllegalStateException("application.yml cannot be loaded");
        Path localConfig = localConfig();
        if (localConfig != null) {
            YamlPropertiesFactoryBean localYaml = new YamlPropertiesFactoryBean();
            localYaml.setResources(new FileSystemResource(localConfig));
            Properties localProperties = localYaml.getObject();
            if (localProperties != null) properties.putAll(localProperties);
        }
        return properties;
    }

    private static Path localConfig() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve("config/application-default.yml");
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        return null;
    }

    static String configuredSecret(Properties config, String name) {
        String value = required(config, name);
        if (value.startsWith("replace-with-")) {
            throw new IllegalStateException("fill " + name + " in config/application-default.yml first");
        }
        return value;
    }

    static String required(Properties config, String name) {
        String value = config.getProperty(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("missing config: " + name);
        return value.strip();
    }
}
