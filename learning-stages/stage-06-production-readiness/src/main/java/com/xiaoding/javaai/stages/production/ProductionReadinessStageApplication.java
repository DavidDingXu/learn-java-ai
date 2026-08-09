package com.xiaoding.javaai.stages.production;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaoding.javaai.stages.support.StageConfig;
import com.xiaoding.javaai.stages.support.StageHttp;
import com.xiaoding.javaai.stages.support.StageOutput;

import java.util.List;

public final class ProductionReadinessStageApplication {

    private ProductionReadinessStageApplication() {
    }

    public static void main(String[] args) {
        StageConfig config = StageConfig.load();
        StageHttp http = new StageHttp();
        List<Service> services = List.of(
                new Service("Knowledge", config.required("stages.knowledge-base-url")),
                new Service("Ticket Agent", config.required("stages.ticket-base-url")),
                new Service("Customer BFF", config.required("stages.customer-base-url"))
        );

        StageOutput.heading("35-39 上线前运行状态");
        for (Service service : services) {
            JsonNode health = http.get(service.baseUrl() + "/actuator/health");
            String metrics = http.text(service.baseUrl() + "/actuator/prometheus");
            StageOutput.value(service.name(), StageOutput.text(health, "status"));
            StageOutput.value(service.name() + " metrics", metrics.lines().filter(line -> !line.startsWith("#")).count());
        }
        System.out.println("\n三个 UP 只说明进程可服务；模型、RAG、Agent 和回滚结果仍要看前面阶段的报告。");
    }

    private record Service(String name, String baseUrl) {
    }
}
