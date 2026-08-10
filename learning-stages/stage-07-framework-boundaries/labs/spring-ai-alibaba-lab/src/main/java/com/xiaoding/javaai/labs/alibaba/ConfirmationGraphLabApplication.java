package com.xiaoding.javaai.labs.alibaba;

import java.util.Properties;

public final class ConfirmationGraphLabApplication {

    private ConfirmationGraphLabApplication() {
    }

    public static void main(String[] args) {
        Properties config = SpringAiAlibabaLabApplication.loadConfig();
        RiskLevel risk = RiskLevel.valueOf(SpringAiAlibabaLabApplication.required(config, "lab.graph.risk"));
        ApprovalDecision approval = ApprovalDecision.valueOf(
                SpringAiAlibabaLabApplication.required(config, "lab.graph.approval"));
        ConfirmationResult result = new ConfirmationGraph().run(risk, approval);
        System.out.printf("risk=%s approval=%s status=%s visited=%s%n",
                risk, approval, result.status(), result.visitedNodes());
    }
}
