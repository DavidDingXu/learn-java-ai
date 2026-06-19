package com.xiaoding.javaai.output;

import com.xiaoding.javaai.output.service.AiJsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiJsonParserTest {

    private final AiJsonParser parser = new AiJsonParser(new ObjectMapper());

    @Test
    void parsesTicketAdviceJson() {
        String rawJson = """
                {
                  "summary": "客户申请退款，但订单已经发货。",
                  "riskLevel": "MEDIUM",
                  "nextActions": ["核对物流状态", "查询退款制度"],
                  "citations": ["policy-refund-001"]
                }
                """;

        TicketAdviceResponse response = parser.parseStrict(rawJson, TicketAdviceResponse.class);

        assertThat(response.summary()).contains("退款");
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(response.nextActions()).contains("核对物流状态");
        assertThat(response.citations()).contains("policy-refund-001");
    }

    @Test
    void rejectsAdviceWithoutRiskLevel() {
        String rawJson = """
                {
                  "summary": "客户申请退款，但订单已经发货。",
                  "nextActions": ["核对物流状态"],
                  "citations": ["policy-refund-001"]
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> parser.parseStrict(rawJson, TicketAdviceResponse.class));
    }

    @Test
    void rejectsAdviceWithoutNextActions() {
        String rawJson = """
                {
                  "summary": "客户申请退款，但订单已经发货。",
                  "riskLevel": "MEDIUM",
                  "nextActions": [],
                  "citations": ["policy-refund-001"]
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> parser.parseStrict(rawJson, TicketAdviceResponse.class));
    }

    @Test
    void rejectsMediumOrHighRiskAdviceWithoutCitation() {
        String rawJson = """
                {
                  "summary": "客户申请退款，但订单已经发货。",
                  "riskLevel": "HIGH",
                  "nextActions": ["转主管审核"],
                  "citations": []
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> parser.parseStrict(rawJson, TicketAdviceResponse.class));
    }

    @Test
    void rejectsUnknownRiskLevel() {
        String rawJson = """
                {
                  "summary": "客户申请退款，但订单已经发货。",
                  "riskLevel": "中风险",
                  "nextActions": ["核对物流状态"],
                  "citations": ["policy-refund-001"]
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> parser.parseStrict(rawJson, TicketAdviceResponse.class));
    }

    @Test
    void rejectsBrokenJson() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseStrict("{summary: 客户退款}", TicketAdviceResponse.class));
    }

    @Test
    void buildsRepairPromptWithoutMarkdownRequirement() {
        String prompt = parser.buildRepairPrompt("{summary: 客户退款}", "TicketAdviceResponse");

        assertThat(prompt).contains("只返回修复后的 JSON");
        assertThat(prompt).contains("不要使用 Markdown 代码块");
    }
}
