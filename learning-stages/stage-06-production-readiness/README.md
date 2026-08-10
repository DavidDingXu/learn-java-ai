# 阶段 06：生产准备

本阶段复用阶段 03-05 已完成的业务服务，并加入安全回归数据、完整评测入口、运行配置和发布检查。Micrometer 指标、并发准入与模型韧性是在前面业务链中逐步加入的，这里把它们放进同一套上线前观察与门禁，而不是再复制一套业务源码。

用 IDEA 打开本阶段根 `pom.xml`，复用项目根目录唯一的 `config/application-default.yml`，再运行三个 Spring Boot 主类。打开 `production-learning-journey.http` 观察健康状态、指标访问边界和输入拒绝；安全与综合评测直接运行本阶段的 `EvalRunner`。

这里的“生产准备”表示代码已具备明确门禁，不表示任何读者环境已经自动满足容量、合规或上线审批要求。

| 篇目 | 先运行或阅读 | 读者会看到 |
|---|---|---|
| 35 | `EvalRunner` 的 `security-eval` | 越权、Prompt 注入和高风险动作负例报告 |
| 36 | HTTP `01-04`、Telemetry 实现 | 三个服务健康；匿名访问 Prometheus 明确返回 401 |
| 37 | `SpringAiKnowledgeAnswerModel`、`SemaphoreAgentRunAdmission` | 超时、重试、并发和未知结果边界 |
| 38 | 三个服务的 `application.yml` | 统一本地覆盖与面向部署的配置层次 |
| 39 | `docs/runbooks/release-checklist.md` | 上线前的配置、数据、容量和回滚检查路径 |
