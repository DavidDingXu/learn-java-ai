package com.xiaoding.javaai.stages.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaoding.javaai.stages.support.StageConfig;
import com.xiaoding.javaai.stages.support.StageHttp;
import com.xiaoding.javaai.stages.support.StageOutput;

import java.util.Map;

public final class ModelEngineeringStageApplication {

    private ModelEngineeringStageApplication() {
    }

    public static void main(String[] args) {
        StageConfig config = StageConfig.load();
        String baseUrl = config.required("stages.knowledge-base-url");
        String question = config.required("stages.question");
        StageHttp http = new StageHttp();

        StageOutput.heading("04-12 真实模型调用");
        JsonNode health = http.get(baseUrl + "/actuator/health");
        StageOutput.value("Knowledge Service", StageOutput.text(health, "status"));

        JsonNode answer = http.post(baseUrl + "/api/v1/knowledge/answers", Map.of("question", question));
        StageOutput.value("问题", question);
        StageOutput.value("模型", StageOutput.text(answer, "model"));
        StageOutput.value("回答", StageOutput.text(answer, "answer"));
        StageOutput.value("引用数", answer.path("citations").size());
        StageOutput.value("拒答", answer.path("refused").asBoolean(false));
    }
}
