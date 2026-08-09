package com.xiaoding.javaai.stages.consultation;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaoding.javaai.stages.support.StageConfig;
import com.xiaoding.javaai.stages.support.StageHttp;
import com.xiaoding.javaai.stages.support.StageOutput;

import java.util.HashMap;
import java.util.Map;

public final class CustomerConsultationStageApplication {

    private CustomerConsultationStageApplication() {
    }

    public static void main(String[] args) {
        StageConfig config = StageConfig.load();
        String baseUrl = config.required("stages.customer-base-url");
        StageHttp http = new StageHttp();

        StageOutput.heading("22-25 C 端咨询");
        JsonNode health = http.get(baseUrl + "/actuator/health");
        StageOutput.value("Customer BFF", StageOutput.text(health, "status"));

        Map<String, Object> request = new HashMap<>();
        request.put("conversationId", null);
        request.put("question", config.required("stages.question"));
        JsonNode answer = http.post(baseUrl + "/api/v1/customer/consultations/answers", request);
        StageOutput.value("会话", StageOutput.text(answer, "conversationId"));
        StageOutput.value("回答尝试", StageOutput.text(answer, "attemptId"));
        StageOutput.value("回答", StageOutput.text(answer, "answer"));
        StageOutput.value("引用数", answer.path("citations").size());
        System.out.println("\n这个 conversationId 会在重试、反馈和升级工单时继续使用。");
    }
}
