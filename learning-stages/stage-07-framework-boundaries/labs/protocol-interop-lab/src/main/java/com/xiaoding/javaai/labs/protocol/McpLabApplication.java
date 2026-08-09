package com.xiaoding.javaai.labs.protocol;

import com.xiaoding.javaai.labs.protocol.mcp.EnterpriseMcpClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class McpLabApplication {

    private McpLabApplication() {
    }

    public static void main(String[] args) throws Exception {
        int port = availablePort();
        String endpoint = "/mcp";
        HttpServletStreamableServerTransportProvider transportProvider =
                HttpServletStreamableServerTransportProvider.builder().mcpEndpoint(endpoint).build();
        Tomcat tomcat = startTomcat(port, transportProvider);

        Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", Map.of("ticketId", Map.of("type", "string")),
                "required", List.of("ticketId"),
                "additionalProperties", false);
        McpServerFeatures.SyncToolSpecification tool = McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder("query_ticket", inputSchema)
                        .description("查询工单摘要")
                        .annotations(McpSchema.ToolAnnotations.builder()
                                .readOnlyHint(true)
                                .destructiveHint(false)
                                .build())
                        .build())
                .callHandler((exchange, request) -> McpSchema.CallToolResult.builder()
                        .addContent(McpSchema.TextContent.builder(
                                "ticket=" + request.arguments().get("ticketId") + ",status=OPEN").build())
                        .build())
                .build();
        var server = McpServer.sync(transportProvider)
                .serverInfo("ticket-mcp", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(tool)
                .build();

        try (var client = McpClient.sync(HttpClientStreamableHttpTransport
                .builder("http://127.0.0.1:" + port)
                .endpoint(endpoint)
                .build())
                .requestTimeout(Duration.ofSeconds(5))
                .build()) {
            EnterpriseMcpClient enterpriseClient = new EnterpriseMcpClient(
                    client, Set.of("query_ticket"));
            var discovery = enterpriseClient.initializeAndDiscover();
            var result = enterpriseClient.callReadTool("query_ticket", Map.of("ticketId", "T-100"));
            System.out.printf("server=%s protocol=%s tools=%s result=%s%n",
                    discovery.serverName(), discovery.protocolVersion(), discovery.registeredTools(),
                    ((McpSchema.TextContent) result.content().getFirst()).text());
        } finally {
            server.closeGracefully();
            transportProvider.closeGracefully().block();
            Schedulers.shutdownNow();
            tomcat.stop();
            tomcat.destroy();
        }
    }

    private static Tomcat startTomcat(int port, jakarta.servlet.Servlet servlet) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(System.getProperty("java.io.tmpdir"));
        Context context = tomcat.addContext("", System.getProperty("java.io.tmpdir"));
        var wrapper = context.createWrapper();
        wrapper.setName("mcpServlet");
        wrapper.setServlet(servlet);
        wrapper.setLoadOnStartup(1);
        wrapper.setAsyncSupported(true);
        context.addChild(wrapper);
        context.addServletMappingDecoded("/*", "mcpServlet");
        tomcat.getConnector().setAsyncTimeout(3000);
        tomcat.start();
        return tomcat;
    }

    private static int availablePort() throws Exception {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(0));
            return socket.getLocalPort();
        }
    }
}
