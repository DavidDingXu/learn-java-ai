package com.xiaoding.javaai.labs.protocol.a2a;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class A2aInteroperabilityTest {

    @Test
    void discoversAgentCardAndReceivesCompletedTaskThroughOfficialClient() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/.well-known/agent-card.json", exchange -> respondJson(exchange, agentCard(baseUrl)));
        server.createContext("/a2a", exchange -> respondJson(exchange, taskResponse(exchange)));
        server.start();

        try (EnterpriseA2aClient client = new EnterpriseA2aClient(baseUrl, Set.of("risk-review"))) {
            A2aDiscoveryReceipt discovery = client.discover();
            var task = client.send("分析工单 T-100 的退款风险");

            assertEquals("risk-review-agent", discovery.agentName());
            assertEquals(Set.of("risk-review"), Set.copyOf(discovery.approvedSkills()));
            assertEquals(TaskState.TASK_STATE_COMPLETED, task.status().state());
            assertEquals("risk=MEDIUM",
                    ((TextPart) task.artifacts().getFirst().parts().getFirst()).text());
        } finally {
            server.stop(0);
        }
    }

    private static String agentCard(String baseUrl) {
        return """
                {
                  "name":"risk-review-agent",
                  "description":"分析工单风险并返回只读结论",
                  "version":"1.0.0",
                  "supportedInterfaces":[
                    {"url":"%s/a2a","protocolBinding":"JSONRPC","tenant":""}
                  ],
                  "capabilities":{"streaming":false,"pushNotifications":false,"extendedAgentCard":false},
                  "defaultInputModes":["text/plain"],
                  "defaultOutputModes":["text/plain"],
                  "skills":[{
                    "id":"risk-review",
                    "name":"Risk Review",
                    "description":"分析工单风险",
                    "tags":["ticket","risk"],
                    "examples":["分析工单 T-100"],
                    "inputModes":["text/plain"],
                    "outputModes":["text/plain"]
                  }]
                }
                """.formatted(baseUrl);
    }

    private static String taskResponse(HttpExchange exchange) throws IOException {
        JsonObject request = JsonParser.parseString(
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        String id = request.get("id").toString();
        return """
                {
                  "jsonrpc":"2.0",
                  "id":%s,
                  "result":{"task":{
                    "id":"remote-task-100",
                    "contextId":"case-T-100",
                    "status":{"state":"TASK_STATE_COMPLETED"},
                    "artifacts":[{
                      "artifactId":"risk-result-1",
                      "name":"risk-result",
                      "parts":[{"text":"risk=MEDIUM"}]
                    }],
                    "metadata":{}
                  }}
                }
                """.formatted(id);
    }

    private static void respondJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
