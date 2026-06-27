package com.xiaoding.javaai.prompt;

import com.xiaoding.javaai.prompt.service.PromptTemplate;
import com.xiaoding.javaai.prompt.service.PromptTemplateService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateServiceTest {

    @Test
    void rendersVariablesAndKeepsVersion() {
        PromptTemplateService service = new PromptTemplateService();
        PromptTemplate template = service.save("ticket-advice", "v1", "工单：{ticket}\n制度：{policy}");

        String rendered = service.render(template.code(), Map.of(
                "ticket", "客户申请退款但订单已发货",
                "policy", "发货后退款需先核对物流状态"
        ));

        assertThat(template.version()).isEqualTo("v1");
        assertThat(rendered).contains("客户申请退款");
        assertThat(rendered).contains("发货后退款需先核对物流状态");
    }

    @Test
    void returnsLatestTemplateByCode() {
        PromptTemplateService service = new PromptTemplateService();
        service.save("ticket-advice", "v1", "旧模板 {ticket}");
        service.save("ticket-advice", "v2", "新模板 {ticket}");

        PromptTemplate latest = service.findLatest("ticket-advice");

        assertThat(latest.version()).isEqualTo("v2");
        assertThat(latest.content()).contains("新模板");
    }

    @Test
    void rollbackPublishesOldVersionAsLatestWithoutOverwritingHistory() {
        PromptTemplateService service = new PromptTemplateService();
        service.save("ticket-advice", "v1", "旧模板 {ticket}");
        service.save("ticket-advice", "v2", "新模板 {ticket}");

        PromptTemplate rolledBack = service.rollback("ticket-advice", "v1");

        assertThat(rolledBack.version()).isEqualTo("rollback-v1");
        assertThat(rolledBack.content()).contains("旧模板");
        assertThat(service.findLatest("ticket-advice").version()).isEqualTo("rollback-v1");
        assertThat(service.findVersion("ticket-advice", "v2").content()).contains("新模板");
    }

    @Test
    void renderRejectsUnresolvedVariables() {
        PromptTemplateService service = new PromptTemplateService();
        service.save("ticket-advice", "v1", "工单：{ticket}\n制度：{policy}");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.render("ticket-advice", Map.of("ticket", "客户申请退款")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unresolved prompt variables")
                .hasMessageContaining("policy");
    }
}
