package com.xiaoding.javaai.legacy;

import com.xiaoding.javaai.legacy.agent.ExternalAgentService;
import com.xiaoding.javaai.legacy.agent.HttpLegacyAgentClient;
import com.xiaoding.javaai.legacy.agent.LegacyAgentClient;
import com.xiaoding.javaai.legacy.agent.model.AgentTaskRequest;
import com.xiaoding.javaai.legacy.agent.model.AgentTaskResult;
import com.xiaoding.javaai.legacy.legacy.InMemoryLegacyAuditLedger;
import com.xiaoding.javaai.legacy.legacy.InMemoryTicketRepository;
import com.xiaoding.javaai.legacy.legacy.LegacyTicketSystem;
import com.xiaoding.javaai.legacy.legacy.LegacyToolApiFacade;
import com.xiaoding.javaai.legacy.legacy.model.OperatorContext;
import com.xiaoding.javaai.legacy.legacy.model.TicketRecord;
import com.xiaoding.javaai.legacy.legacy.model.TicketSnapshot;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacyAiIntegrationTest {

    @Test
    void legacySystemShouldKeepPermissionAndSubmitAgentTaskOnly() {
        InMemoryTicketRepository repository = repositoryWithRefundTicket();
        InMemoryLegacyAuditLedger auditLedger = new InMemoryLegacyAuditLedger();
        CapturingAgentClient agentClient = new CapturingAgentClient();
        LegacyTicketSystem legacyTicketSystem = new LegacyTicketSystem(repository, auditLedger, agentClient);

        AgentTaskResult result = legacyTicketSystem.requestAiAdvice(
                "T-1001",
                "这个客户申请退款，但订单已经发货，应该怎么处理？",
                supportOperator()
        );

        assertThat(result.getAdvice()).contains("先查询制度");
        assertThat(agentClient.lastRequest.getTicketId()).isEqualTo("T-1001");
        assertThat(agentClient.lastRequest.getQuestion()).contains("申请退款");
        assertThat(agentClient.lastRequest.getOperatorContext().getOperatorId()).isEqualTo("u1001");
        assertThat(auditLedger.records()).hasSize(1);
        assertThat(auditLedger.records().get(0).getAction()).isEqualTo("SUBMIT_AGENT_TASK");
    }

    @Test
    void legacyToolApiShouldFilterUnauthorizedDepartment() {
        InMemoryTicketRepository repository = repositoryWithRefundTicket();
        LegacyToolApiFacade toolApi = new LegacyToolApiFacade(repository);

        assertThatThrownBy(() -> {
            toolApi.queryTicket("T-1001", financeOperator());
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operator cannot access ticket");
    }

    @Test
    void externalAgentShouldUseLegacyToolApiInsteadOfLegacyInternalClasses() {
        InMemoryTicketRepository repository = repositoryWithRefundTicket();
        LegacyToolApiFacade toolApi = new LegacyToolApiFacade(repository);
        ExternalAgentService agentService = new ExternalAgentService(toolApi);

        AgentTaskResult result = agentService.handle(new AgentTaskRequest(
                "task-T-1001",
                "T-1001",
                "帮我判断这个退款工单，必要时关闭工单",
                supportOperator()
        ));

        assertThat(result.getAdvice()).contains("订单已发货");
        assertThat(result.isRequiresHumanApproval()).isTrue();
        assertThat(result.getToolSnapshots()).extracting(TicketSnapshot::getTicketId).containsExactly("T-1001");
    }

    @Test
    void legacySystemCanCallRealExternalAgentServiceOverHttp() throws Exception {
        final String[] capturedRequestBody = new String[1];
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/helpdesk-agent/advice/live", exchange -> {
            capturedRequestBody[0] = read(exchange.getRequestBody());
            byte[] body = ("{"
                    + "\"context\":{\"requiredAction\":\"MANUAL_REVIEW\",\"trace\":{\"traceId\":\"trace-live\"}},"
                    + "\"answer\":{\"content\":\"真实模型建议：先核对物流，再按退款制度转人工复核。\"}"
                    + "}").getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            HttpLegacyAgentClient agentClient = new HttpLegacyAgentClient(new URL(
                    "http://localhost:" + server.getAddress().getPort() + "/api/helpdesk-agent/advice/live"
            ));
            AgentTaskResult result = agentClient.requestAdvice(new AgentTaskRequest(
                    "task-T-1001",
                    "T-1001",
                    "客户申请退款但订单已发货怎么办",
                    supportOperator()
            ));

            assertThat(capturedRequestBody[0]).contains("\"ticketId\":\"T-1001\"");
            assertThat(capturedRequestBody[0]).contains("\"department\":\"support\"");
            assertThat(result.getAdvice()).contains("真实模型建议");
            assertThat(result.isRequiresHumanApproval()).isTrue();
            assertThat(result.getTraceId()).isEqualTo("trace-live");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void agentTaskApiAndToolApiShouldStayAsSeparateContracts() {
        InMemoryTicketRepository repository = repositoryWithRefundTicket();
        InMemoryLegacyAuditLedger auditLedger = new InMemoryLegacyAuditLedger();
        CapturingAgentClient agentClient = new CapturingAgentClient();
        LegacyTicketSystem legacyTicketSystem = new LegacyTicketSystem(repository, auditLedger, agentClient);
        LegacyToolApiFacade toolApi = new LegacyToolApiFacade(repository);

        legacyTicketSystem.requestAiAdvice(
                "T-1001",
                "请给出退款工单建议",
                supportOperator()
        );
        TicketSnapshot snapshot = toolApi.queryTicket("T-1001", supportOperator());

        assertThat(agentClient.lastRequest.getContractName()).isEqualTo("AgentTask API");
        assertThat(snapshot.getContractName()).isEqualTo("Legacy Tool API");
        assertThat(agentClient.lastRequest.getQuestion()).contains("退款工单");
        assertThat(snapshot.getContent()).contains("物流显示运输中");
    }

    @Test
    void auditShouldKeepOperatorTenantDepartmentsAndPermissionsSnapshot() {
        InMemoryTicketRepository repository = repositoryWithRefundTicket();
        InMemoryLegacyAuditLedger auditLedger = new InMemoryLegacyAuditLedger();
        CapturingAgentClient agentClient = new CapturingAgentClient();
        LegacyTicketSystem legacyTicketSystem = new LegacyTicketSystem(repository, auditLedger, agentClient);

        legacyTicketSystem.requestAiAdvice(
                "T-1001",
                "请给出退款工单建议",
                supportOperator()
        );

        assertThat(auditLedger.records()).singleElement().satisfies(record -> {
            assertThat(record.getTenantId()).isEqualTo("tenant-a");
            assertThat(record.getDepartments()).containsExactly("support");
            assertThat(record.getPermissions()).containsExactlyInAnyOrder("AI_TICKET_ADVICE", "TICKET_READ");
        });
    }

    private static InMemoryTicketRepository repositoryWithRefundTicket() {
        InMemoryTicketRepository repository = new InMemoryTicketRepository();
        repository.save(new TicketRecord(
                "T-1001",
                "tenant-a",
                "support",
                "OPEN",
                "客户申请退款，但订单已经发货，物流显示运输中。"
        ));
        return repository;
    }

    private static OperatorContext supportOperator() {
        return new OperatorContext(
                "u1001",
                "tenant-a",
                new HashSet<String>(Arrays.asList("support")),
                new HashSet<String>(Arrays.asList("AI_TICKET_ADVICE", "TICKET_READ"))
        );
    }

    private static OperatorContext financeOperator() {
        return new OperatorContext(
                "u2001",
                "tenant-a",
                new HashSet<String>(Arrays.asList("finance")),
                new HashSet<String>(Arrays.asList("AI_TICKET_ADVICE", "TICKET_READ"))
        );
    }

    private static String read(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), "UTF-8");
    }

    private static final class CapturingAgentClient implements LegacyAgentClient {
        private AgentTaskRequest lastRequest;

        @Override
        public AgentTaskResult requestAdvice(AgentTaskRequest request) {
            this.lastRequest = request;
            return AgentTaskResult.completed(
                    request.getTaskId(),
                    "先查询制度，再通过 Tool API 查询订单和工单状态。",
                    false,
                    "trace-demo",
                    java.util.Collections.<TicketSnapshot>emptyList()
            );
        }
    }
}
