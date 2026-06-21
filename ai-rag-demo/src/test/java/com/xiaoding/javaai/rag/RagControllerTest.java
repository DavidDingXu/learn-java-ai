package com.xiaoding.javaai.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesReadmeAnswerEndpointThroughHttpBinding() throws Exception {
        String body = """
                {
                  "query": "客户申请退款但订单已发货怎么办",
                  "tenantId": "tenant-a",
                  "department": "support"
                }
                """;

        mockMvc.perform(post("/api/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("退款")))
                .andExpect(jsonPath("$.citations[0].documentId").value("refund-policy-001"));
    }

    @Test
    void exposesRagPipelineLabEndpointThroughHttpBinding() throws Exception {
        mockMvc.perform(post("/api/rag/lab/pipeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentId": "refund-policy-lab",
                                  "tenantId": "tenant-a",
                                  "title": "退款制度样例",
                                  "docType": "POLICY",
                                  "departments": ["support"],
                                  "markdown": "# 退款制度\\n## 发货后退款\\n客户申请退款但订单已经发货时，客服必须先核对物流状态。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plainText", containsString("发货后退款")))
                .andExpect(jsonPath("$.chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.chunks[0].chunkId").value("refund-policy-lab-c1"))
                .andExpect(jsonPath("$.chunks[0].headingPath[0]").value("退款制度"));
    }

    @Test
    void exposesRagAccessAndRetrievalLabEndpointsThroughHttpBinding() throws Exception {
        mockMvc.perform(post("/api/rag/lab/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant-a",
                                  "department": "support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisions[*].reason", hasItem("allowed")))
                .andExpect(jsonPath("$.decisions[*].reason", hasItem("department_mismatch")))
                .andExpect(jsonPath("$.decisions[*].reason", hasItem("tenant_mismatch")));

        mockMvc.perform(post("/api/rag/lab/retrieval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "客户申请退款但订单已发货怎么办",
                                  "tenantId": "tenant-a",
                                  "department": "support",
                                  "topK": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hybridResults[0].chunkId").value("refund-policy-001-c1"))
                .andExpect(jsonPath("$.hybridResults[0].sources", hasItem("keyword")))
                .andExpect(jsonPath("$.rerankedResults[0].reasons", hasItem("content")));
    }

    @Test
    void exposesRagRewriteAndIndexLabEndpointsThroughHttpBinding() throws Exception {
        mockMvc.perform(post("/api/rag/lab/rewrite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "大额退款已经发货怎么办",
                                  "tenantId": "tenant-a",
                                  "department": "support",
                                  "topK": 3,
                                  "maxCharsPerChunk": 42
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewrittenQueries", hasItem("发货后退款处理规则")))
                .andExpect(jsonPath("$.rewriteReasons", hasItem("detected shipping status")))
                .andExpect(jsonPath("$.compressedItems[0].compressedContent", containsString("退款")));

        mockMvc.perform(post("/api/rag/lab/index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentId": "refund-policy-lab",
                                  "markdown": "# 退款制度\\n## 发货后退款\\n客户申请退款但订单已经发货时，客服必须先核对物流状态。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.chunkCount", greaterThan(0)));
    }
}
