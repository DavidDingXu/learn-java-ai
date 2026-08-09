package com.xiaoding.javaai.stages.framework;

import com.xiaoding.javaai.stages.support.StageOutput;

import java.util.List;

public final class FrameworkBoundariesStageApplication {

    private FrameworkBoundariesStageApplication() {
    }

    public static void main(String[] args) {
        StageOutput.heading("40-51 框架与协议边界实验");
        List.of(
                new Experiment("Spring AI Alibaba", "Provider、Embedding、Graph 迁移边界", "labs/spring-ai-alibaba-lab"),
                new Experiment("LangChain4j", "AI Services、RAG、Tool 适配边界", "labs/langchain4j-lab"),
                new Experiment("AgentScope", "Tool 裁决与拆分决策", "labs/agentscope-lab"),
                new Experiment("MCP / A2A", "发现、准入、任务和失败语义", "labs/protocol-interop-lab")
        ).forEach(experiment -> System.out.printf(
                "%-20s %-34s %s%n",
                experiment.name(), experiment.question(), experiment.source()
        ));
        System.out.println("\n这一阶段输出迁移决策，不声称另一套业务系统已经跑通。");
    }

    private record Experiment(String name, String question, String source) {
    }
}
