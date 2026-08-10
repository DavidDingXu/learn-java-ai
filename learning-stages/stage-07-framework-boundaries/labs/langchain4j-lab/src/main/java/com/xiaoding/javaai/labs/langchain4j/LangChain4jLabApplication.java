package com.xiaoding.javaai.labs.langchain4j;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class LangChain4jLabApplication {

    private LangChain4jLabApplication() {
    }

    public static void main(String[] args) {
        Properties config = loadConfig();
        OpenAiChatModel model = createModel(config);
        System.out.println(new LangChain4jPolicyAnswerAdapter(model)
                .answer(new PolicyQuestion("tenant-a", "退款审核通过后通常多久到账？")));
    }

    static OpenAiChatModel createModel(Properties config) {
        return OpenAiChatModel.builder()
                .apiKey(configuredSecret(config, "lab.openai.api-key"))
                .baseUrl(required(config, "lab.openai.base-url"))
                .modelName(required(config, "lab.openai.model"))
                .temperature(0.1)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(0)
                .build();
    }

    static void runRag(OpenAiChatModel model) {
        KnowledgeSearchPort search = (scope, question, topK) -> List.of(
                new KnowledgeSnippet("refund-arrival-time",
                        "退款审核通过后通常需要 1 到 5 个工作日原路到账。"));
        PolicyAnswer answer = new TenantScopedRagAdapter(search, model, 3).answer(
                new KnowledgeAccessScope("tenant-a", "employee-7", List.of("service")),
                "退款审核通过后通常多久到账？");
        System.out.println(answer);
    }

    static void runTool(OpenAiChatModel model) {
        TicketReadTools tools = new TicketReadTools(ticketId ->
                new TicketSnapshot(ticketId, "OPEN", "客户等待退款到账"));
        TicketDecision decision = new LangChain4jTicketDecisionAdapter(model, tools)
                .decide("T-100", "读取工单事实后判断下一步，只允许只读查询");
        System.out.printf("decision=%s toolCalls=%d%n", decision, tools.invocationCount());
    }

    static Properties loadConfig() {
        try (InputStream input = LangChain4jLabApplication.class.getResourceAsStream("/application.properties")) {
            if (input == null) throw new IllegalStateException("application.properties cannot be loaded");
            Properties config = new Properties();
            config.load(input);
            applyLocalOverride(config);
            return config;
        } catch (IOException error) {
            throw new IllegalStateException("application.properties cannot be loaded", error);
        }
    }

    private static void applyLocalOverride(Properties config) throws IOException {
        Path localConfig = localConfig();
        if (localConfig == null) return;
        applyLocalOverride(config, localConfig);
    }

    static void applyLocalOverride(Properties config, Path localConfig) throws IOException {
        try (InputStream input = Files.newInputStream(localConfig)) {
            Object loaded = new Yaml().load(input);
            if (!(loaded instanceof Map<?, ?> root)) return;
            Map<?, ?> openai = nested(nested(nested(root, "spring"), "ai"), "openai");
            copyText(openai, "api-key", config, "lab.openai.api-key");
            copyText(openai, "base-url", config, "lab.openai.base-url");
            copyText(nested(openai, "chat"), "model", config, "lab.openai.model");
        }
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

    private static Map<?, ?> nested(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return value instanceof Map<?, ?> nested ? nested : Map.of();
    }

    private static void copyText(Map<?, ?> source, String key, Properties target, String targetKey) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) target.setProperty(targetKey, text);
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
