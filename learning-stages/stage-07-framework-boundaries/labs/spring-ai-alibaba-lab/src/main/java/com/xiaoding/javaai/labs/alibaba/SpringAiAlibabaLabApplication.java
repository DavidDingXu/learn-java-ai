package com.xiaoding.javaai.labs.alibaba;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public final class SpringAiAlibabaLabApplication {

    private SpringAiAlibabaLabApplication() {
    }

    public static void main(String[] args) {
        Properties config = loadConfig();
        switch (required(config, "lab.mode")) {
            case "provider" -> runProvider(config);
            case "retrieval" -> runRetrieval(config);
            case "graph" -> runGraph(config);
            case "compatibility" -> runCompatibility(config);
            default -> throw new IllegalArgumentException(
                    "lab.mode must be provider, retrieval, graph or compatibility");
        }
    }

    private static void runProvider(Properties config) {
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
        System.out.printf("model=%s answer=%s metadata=%s%n",
                answer.model(), answer.text(), answer.providerMetadata());
    }

    private static void runRetrieval(Properties config) {
        DomesticRetrievalProfile profile = new DomesticRetrievalProfile(
                required(config, "lab.dashscope.embedding-model"),
                Integer.parseInt(required(config, "lab.dashscope.embedding-dimensions")),
                required(config, "lab.dashscope.rerank-model"),
                Integer.parseInt(required(config, "lab.dashscope.rerank-top-n")));
        RetrievalReplacementExperiment experiment = new RetrievalReplacementExperiment(profile);
        RetrievalExperimentReport report = experiment.evaluate(List.of(
                new RetrievalGoldenCase("refund-1", List.of("refund-policy"),
                        List.of("refund-policy", "shipping-policy"), Duration.ofMillis(85)),
                new RetrievalGoldenCase("invoice-1", List.of("invoice-policy"),
                        List.of("shipping-policy", "invoice-policy"), Duration.ofMillis(120))));
        System.out.printf("embedding=%s rerank=%s recall=%.3f mrr=%.3f p95=%s%n",
                experiment.embeddingOptions().getModel(), experiment.rerankOptions().getModel(),
                report.recallAtK(), report.mrr(), report.p95Latency());
    }

    private static void runGraph(Properties config) {
        RiskLevel risk = RiskLevel.valueOf(required(config, "lab.graph.risk"));
        ApprovalDecision approval = ApprovalDecision.valueOf(required(config, "lab.graph.approval"));
        ConfirmationResult result = new ConfirmationGraph().run(risk, approval);
        System.out.printf("status=%s visited=%s%n", result.status(), result.visitedNodes());
    }

    private static void runCompatibility(Properties config) {
        FrameworkCompatibilityDecision decision = FrameworkCompatibilityDecision.compare(
                new FrameworkBaseline("Spring AI", "2.0.0", "4.1.0"),
                new FrameworkBaseline("Spring AI Alibaba",
                        required(config, "lab.compatibility.candidate-version"),
                        required(config, "lab.compatibility.candidate-boot-line")));
        System.out.printf("inPlace=%s boundary=%s reasons=%s%n",
                decision.inPlaceCompatible(), decision.boundary(), decision.reasons());
    }

    private static Properties loadConfig() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();
        if (properties == null) throw new IllegalStateException("application.yml cannot be loaded");
        return properties;
    }

    private static String configuredSecret(Properties config, String name) {
        String value = required(config, name);
        if (value.startsWith("replace-with-")) {
            throw new IllegalStateException("fill " + name + " in application.yml first");
        }
        return value;
    }

    private static String required(Properties config, String name) {
        String value = config.getProperty(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("missing config: " + name);
        return value.strip();
    }
}
