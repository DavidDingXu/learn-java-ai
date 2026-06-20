package com.xiaoding.javaai.eval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EvalControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesBasicEvalEndpointThroughHttpBinding() throws Exception {
        mockMvc.perform(post("/api/eval/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "caseId": "E-001",
                                    "question": "发货后退款怎么处理",
                                    "expectedKeyword": "核对物流",
                                    "actualAnswer": "建议先核对物流状态，再转人工复核。"
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.passed").value(1))
                .andExpect(jsonPath("$.passRate").value(1));
    }

    @Test
    void exposesRagAndAgentEvalEndpointsThroughHttpBinding() throws Exception {
        mockMvc.perform(post("/api/eval/rag/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cases": [
                                    {
                                      "caseId": "R-001",
                                      "question": "发货后退款怎么办",
                                      "expectedChunkIds": ["refund-policy-001-c1"],
                                      "expectNoEvidence": false
                                    }
                                  ],
                                  "observations": [
                                    {
                                      "caseId": "R-001",
                                      "retrievedChunkIds": ["refund-policy-001-c1"],
                                      "citedChunkIds": ["refund-policy-001-c1"],
                                      "noEvidenceReturned": false,
                                      "answer": "发货后先核对物流状态。"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.passRate").value(1));

        mockMvc.perform(post("/api/eval/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cases": [
                                    {
                                      "caseId": "A-001",
                                      "question": "关闭高风险退款工单",
                                      "expectedToolPath": ["ticket.lookup", "order.lookup", "policy.search"],
                                      "expectHumanApproval": true
                                    }
                                  ],
                                  "observations": [
                                    {
                                      "caseId": "A-001",
                                      "actualToolPath": ["ticket.lookup", "order.lookup", "policy.search"],
                                      "humanApprovalRequested": true,
                                      "riskLevel": "HIGH"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.passRate").value(1));
    }
}
