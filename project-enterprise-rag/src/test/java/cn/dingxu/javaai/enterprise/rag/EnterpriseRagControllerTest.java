package cn.dingxu.javaai.enterprise.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnterpriseRagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadsDocumentAndAnswersWithCitation() throws Exception {
        String upload = """
                {
                  "documentId": "refund-policy-2026",
                  "tenantId": "tenant-a",
                  "departments": ["support"],
                  "type": "POLICY",
                  "content": "退款制度\\n发货后退款需先核对物流状态。\\n高金额退款必须转人工复核。"
                }
                """;

        mockMvc.perform(post("/api/enterprise-rag/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.documentId").value("refund-policy-2026"))
                .andExpect(jsonPath("$.chunkCount").value(2));

        String question = """
                {
                  "question": "客户申请退款但订单已发货怎么办",
                  "tenantId": "tenant-a",
                  "department": "support"
                }
                """;

        mockMvc.perform(post("/api/enterprise-rag/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(question))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("发货后退款需先核对物流状态")))
                .andExpect(jsonPath("$.citations[0].documentId").value("refund-policy-2026"))
                .andExpect(jsonPath("$.trace.steps[*].name", hasItem("hybrid-retrieve")));
    }

    @Test
    void evaluatesRagQualityThroughHttpEndpoint() throws Exception {
        mockMvc.perform(post("/api/enterprise-rag/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "documentId": "refund-policy-2026",
                          "tenantId": "tenant-a",
                          "departments": ["support"],
                          "type": "POLICY",
                          "content": "退款制度\\n发货后退款需先核对物流状态。"
                        }
                        """));

        String body = """
                [
                  {
                    "caseId": "E-001",
                    "question": "发货后退款怎么处理",
                    "tenantId": "tenant-a",
                    "department": "support",
                    "expectedDocumentId": "refund-policy-2026"
                  }
                ]
                """;

        mockMvc.perform(post("/api/enterprise-rag/eval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCount").value(1))
                .andExpect(jsonPath("$.retrievalHitRate").value(1.00))
                .andExpect(jsonPath("$.citationHitRate").value(1.00));
    }
}
