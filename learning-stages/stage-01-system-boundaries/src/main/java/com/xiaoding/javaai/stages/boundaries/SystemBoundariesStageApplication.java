package com.xiaoding.javaai.stages.boundaries;

import com.xiaoding.javaai.stages.support.StageOutput;

import java.util.List;

public final class SystemBoundariesStageApplication {

    private SystemBoundariesStageApplication() {
    }

    public static void main(String[] args) {
        StageOutput.heading("01-03 系统边界");
        List.of(
                new Boundary("Knowledge Service", "文档、版本、检索与引用", "工单状态"),
                new Boundary("Customer BFF", "客户身份、会话与协议聚合", "知识和工单事实"),
                new Boundary("Ticket Agent", "协同任务、确认单与审计", "直接改 CRM 数据"),
                new Boundary("JDK8 CRM", "最终业务状态", "模型编排")
        ).forEach(boundary -> System.out.printf(
                "%-20s owns=%-24s refuses=%s%n",
                boundary.component(), boundary.owns(), boundary.refuses()
        ));
        System.out.println("\n下一阶段只启动 Knowledge Service，先跑通一条真实模型调用。");
    }

    private record Boundary(String component, String owns, String refuses) {
    }
}
