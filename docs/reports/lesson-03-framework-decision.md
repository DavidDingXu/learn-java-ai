# Lesson 03 Framework Decision

## Review Scope

本次复核回答两个问题：为什么 Java 21 主线选择 Spring AI，以及 Spring AI Alibaba、LangChain4j、AgentScope、Provider SDK 和公司既有 AI 平台应放在哪个边界。它不把文档能力列表当成项目运行结果。

## Reviewed Files

- `pom.xml`
- `services/knowledge-service/pom.xml`
- `apps/customer-bff/pom.xml`
- `services/ticket-agent-service/pom.xml`
- `labs/pom.xml`
- `labs/spring-ai-alibaba-lab/pom.xml`
- `labs/langchain4j-lab/pom.xml`
- `labs/agentscope-lab/pom.xml`
- `docs/adr/0001-spring-ai-mainline.md`
- `docs/adr/0002-build-boundaries.md`
- `docs/decisions/framework-selection-matrix.md`
- Spring AI、Spring AI Alibaba、LangChain4j 和 AgentScope 官方文档

## Commit

提交基线：`3be7f19`。该提交证明 Spring AI Provider starter 只进入 Knowledge Service，三个对照框架仍在独立 labs reactor。

## Accepted Boundaries

- Spring AI 作为 Java 21 主线，原因是与现有 Spring Boot 工程体系的综合迁移成本最低。
- Spring AI 类型限制在基础设施适配器，业务层通过 `KnowledgeAnswerModel` 一类端口隔离。
- 不建设万能模型网关，也不把 Chat、RAG、Tool 和 Agent 压成统一的 `String -> String`。
- Spring AI Alibaba、LangChain4j 和 AgentScope 使用同一业务合同、同一数据集在独立 labs 中比较。
- Provider SDK 只在主线缺失关键能力时进入窄适配器。
- 公司已有强制 AI 平台时，平台合同优先，不能为了课程示例绕过公司治理。

## Rejected Alternatives

- 拒绝在主 reactor 同时引入四套框架：依赖和运行时语义会污染生产主线。
- 拒绝按框架各做一套业务 Demo：无法比较迁移成本，也无法形成连续项目。
- 拒绝 Controller 直接调用 Spring AI 或 Provider SDK：业务代码会绑定供应商协议。
- 拒绝只按功能勾选数量选框架：安全、观测、版本治理和退出成本同样决定公司落地难度。

## Replacement Conditions

- Spring AI 无法承载必需的 Provider 能力，且窄适配器也无法补齐。
- 公司 AI 平台已提供更稳定的身份、配额、路由、审计和 SLA 合同。
- 其他框架在相同业务合同和数据集上取得可重复的质量或维护成本优势。
- 当前版本组合不再满足安全、兼容或长期维护要求。

## Evidence Limit

本报告确认的是依赖边界和决策依据，不代表四个框架的业务实验已经完成。框架迁移结论必须等待对应 labs 的代码、Trace、评测数据和失败案例。
