package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import org.yaml.snakeyaml.Yaml;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class AgentScopeLabApplication {

    private AgentScopeLabApplication() {
    }

    public static void main(String[] args) {
        try {
            Properties config = loadConfig();
            OpenAIChatModel model = createModel(config);
            Toolkit toolkit = new Toolkit();
            toolkit.registerTool(new TicketBusinessTools());
            PermissionContextState permissions = PermissionContextState.builder()
                    .mode(PermissionMode.DEFAULT)
                    .addAllowRule("query_ticket",
                            new PermissionRule("query_ticket", null, PermissionBehavior.ALLOW, "ticket-policy"))
                    .build();

            try (ReActAgent agent = ReActAgent.builder()
                    .name("ticket-reader")
                    .sysPrompt("你是工单只读助手。必须先调用 query_ticket 获取事实，再用中文回答。")
                    .model(model)
                    .toolkit(toolkit)
                    .permissionContext(permissions)
                    .maxIters(4)
                    .build()) {
                RuntimeContext context = RuntimeContext.builder()
                        .sessionId("tenant-a:T-100")
                        .userId("employee-7")
                        .build();
                Msg answer = agent.call("查询工单 T-100 当前状态", context).block();
                if (answer == null) throw new IllegalStateException("AgentScope returned no answer");
                System.out.println(answer.getTextContent());
            }
        } finally {
            Schedulers.shutdownNow();
        }
    }

    static OpenAIChatModel createModel(Properties config) {
        return OpenAIChatModel.builder()
                .apiKey(configuredSecret(config, "lab.openai.api-key"))
                .baseUrl(required(config, "lab.openai.base-url"))
                .modelName(required(config, "lab.openai.model"))
                .stream(false)
                .build();
    }

    static Properties loadConfig() {
        try (InputStream input = AgentScopeLabApplication.class.getResourceAsStream("/application.properties")) {
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
