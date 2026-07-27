package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;
import com.xiaoding.javaai.ticket.agent.domain.PreparedToolCall;
import com.xiaoding.javaai.ticket.agent.domain.ToolEffect;
import com.xiaoding.javaai.ticket.agent.domain.ToolRisk;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessToolCatalogTest {

    private final BusinessToolCatalog catalog = BusinessToolCatalog.standard(
            Set.of("refund-review", "tier-2"));

    @Test
    void prepares_a_write_action_from_allowlisted_arguments_and_server_side_policy() {
        PreparedToolCall call = catalog.prepare(new AgentDecision.UseTool(
                "ASSIGN_QUEUE",
                Map.of("queueCode", " refund-review "),
                "需要退款专席处理"));

        assertThat(call.effect()).isEqualTo(ToolEffect.WRITE);
        assertThat(call.risk()).isEqualTo(ToolRisk.MEDIUM);
        assertThat(call.requiredRole()).isEqualTo("TICKET_OPERATOR");
        assertThat(call.arguments()).containsExactlyEntriesOf(Map.of("queueCode", "refund-review"));
    }

    @Test
    void rejects_identity_or_permission_arguments_injected_by_the_model() {
        AgentDecision.UseTool proposal = new AgentDecision.UseTool(
                "QUERY_KNOWLEDGE",
                Map.of("question", "退款多久到账？", "tenantId", "tenant-b"),
                "查询制度");

        assertThatThrownBy(() -> catalog.prepare(proposal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void rejects_a_queue_that_is_not_in_server_configuration() {
        AgentDecision.UseTool proposal = new AgentDecision.UseTool(
                "ASSIGN_QUEUE", Map.of("queueCode", "model-created-queue"), "分配队列");

        assertThatThrownBy(() -> catalog.prepare(proposal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queueCode");
    }

    @Test
    void rejects_an_unknown_tool() {
        AgentDecision.UseTool proposal = new AgentDecision.UseTool(
                "GRANT_ADMIN", Map.of(), "提升权限");

        assertThatThrownBy(() -> catalog.prepare(proposal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown tool");
    }

    @Test
    void rejects_an_invalid_refund_amount() {
        AgentDecision.UseTool proposal = new AgentDecision.UseTool(
                "ISSUE_REFUND", Map.of("amountMinor", "-100", "currency", "CNY"), "发起退款");

        assertThatThrownBy(() -> catalog.prepare(proposal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amountMinor");
    }

    @Test
    void rejects_an_unsupported_refund_currency() {
        AgentDecision.UseTool proposal = new AgentDecision.UseTool(
                "ISSUE_REFUND", Map.of("amountMinor", "100", "currency", "BTC"), "发起退款");

        assertThatThrownBy(() -> catalog.prepare(proposal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }
}
