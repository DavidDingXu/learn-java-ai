package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public final class PolicyBusinessTools {

    @Tool(name = "query_refund_policy", description = "查询退款到账制度摘要", readOnly = true)
    public String queryRefundPolicy(
            @ToolParam(name = "policy_id", description = "制度编号") String policyId) {
        return "{\"policyId\":\"" + policyId
                + "\",\"arrivalTime\":\"审核通过后一到五个工作日原路到账\"}";
    }
}
