package com.xiaoding.javaai.stages.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaoding.javaai.stages.support.StageConfig;
import com.xiaoding.javaai.stages.support.StageHttp;
import com.xiaoding.javaai.stages.support.StageOutput;

import java.util.Map;
import java.util.UUID;

public final class ControlledAgentStageApplication {

    private ControlledAgentStageApplication() {
    }

    public static void main(String[] args) {
        StageConfig config = StageConfig.load();
        String baseUrl = config.required("stages.ticket-base-url");
        StageHttp http = new StageHttp();

        StageOutput.heading("26-34 受控 Agent");
        StageOutput.value("Ticket Agent", StageOutput.text(http.get(baseUrl + "/actuator/health"), "status"));

        String idempotencyKey = "reader-" + UUID.randomUUID();
        JsonNode created = http.post(
                baseUrl + "/api/v1/agent/tasks",
                Map.of(
                        "caseId", "reader-refund-case",
                        "objective", "查询退款政策，并根据证据给出下一步建议",
                        "businessContext", Map.of("question", config.required("stages.question"))
                ),
                Map.of("Idempotency-Key", idempotencyKey)
        );
        String taskId = StageOutput.text(created, "taskId");
        StageOutput.value("任务", taskId);
        StageOutput.value("初始状态", StageOutput.text(created, "status"));

        JsonNode run = http.postWithoutBody(baseUrl + "/api/v1/agent/tasks/" + taskId + "/runs");
        StageOutput.value("运行后状态", StageOutput.text(run, "status"));
        StageOutput.value("结果", StageOutput.text(run, "outcome"));
        if (!run.path("confirmation").isMissingNode() && !run.path("confirmation").isNull()) {
            StageOutput.value("待确认工具", run.path("confirmation").path("toolName").asText());
            StageOutput.value("风险级别", run.path("confirmation").path("risk").asText());
        }
    }
}
