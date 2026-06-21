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

    @Test
    void exposesJudgePromptAndHarnessEvalEndpointsThroughHttpBinding() throws Exception {
        mockMvc.perform(post("/api/eval/judge/calibrate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "caseId": "J-001",
                                    "question": "发货后退款是否通过",
                                    "humanPassed": true,
                                    "judgePassed": true,
                                    "judgeConfidence": 0.91,
                                    "note": "人工和裁判一致"
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.agreementRate").value(1));

        mockMvc.perform(post("/api/eval/prompt/regression")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "caseId": "P-001",
                                    "promptKey": "refund-advice",
                                    "baselineVersion": "v1",
                                    "candidateVersion": "v2",
                                    "baselinePassRate": 0.86,
                                    "candidatePassRate": 0.88,
                                    "tolerance": 0.02
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.passRate").value(1));

        mockMvc.perform(post("/api/eval/harness/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cases": [
                                    {
                                      "caseId": "H-001",
                                      "scenario": "hybrid-rag",
                                      "minQualityImprovement": 0.05,
                                      "maxCostIncreaseRate": 0.30,
                                      "maxLatencyIncreaseRate": 0.30
                                    }
                                  ],
                                  "observations": [
                                    {
                                      "caseId": "H-001",
                                      "strategy": "baseline",
                                      "qualityScore": 0.72,
                                      "costUnits": 100,
                                      "latencyMillis": 800
                                    },
                                    {
                                      "caseId": "H-001",
                                      "strategy": "candidate",
                                      "qualityScore": 0.82,
                                      "costUnits": 118,
                                      "latencyMillis": 920
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.promotableCandidates").value(1));
    }
}
