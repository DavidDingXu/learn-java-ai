package cn.dingxu.javaai.legacy;

import cn.dingxu.javaai.legacy.agent.ExternalAgentService;
import cn.dingxu.javaai.legacy.agent.LegacyAgentClient;
import cn.dingxu.javaai.legacy.agent.model.AgentTaskRequest;
import cn.dingxu.javaai.legacy.agent.model.AgentTaskResult;
import cn.dingxu.javaai.legacy.legacy.InMemoryLegacyAuditLedger;
import cn.dingxu.javaai.legacy.legacy.InMemoryTicketRepository;
import cn.dingxu.javaai.legacy.legacy.LegacyTicketSystem;
import cn.dingxu.javaai.legacy.legacy.LegacyToolApiFacade;
import cn.dingxu.javaai.legacy.legacy.model.OperatorContext;
import cn.dingxu.javaai.legacy.legacy.model.TicketRecord;
import cn.dingxu.javaai.legacy.legacy.model.TicketSnapshot;
import org.junit.jupiter.api.Test;

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
