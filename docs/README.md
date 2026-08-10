# 文档导航

本页按问题类型组织项目文档。首次使用可以先阅读系统上下文和本地启动说明，再根据要修改的能力进入 ADR、Runbook 或验证档案。

## 架构与决策

| 文档 | 解决的问题 |
|---|---|
| [系统上下文](architecture/system-context.md) | Web、BFF、知识服务、Agent 和老系统如何协作 |
| [客户咨询时序](architecture/customer-consultation-flow.md) | 流式回答、重试、转工单和人工确认的完整链路 |
| [服务与数据所有权](architecture/service-ownership.md) | 每类业务数据由哪个服务负责 |
| [ADR-0001：Spring AI 主线](adr/0001-spring-ai-mainline.md) | 为什么主应用选择 Spring AI |
| [ADR-0002：构建边界](adr/0002-build-boundaries.md) | 为什么拆分 Java 21、Java 8、labs 和前端构建 |
| [ADR-0003：框架实验隔离](adr/0003-framework-lab-isolation.md) | 如何防止候选框架污染主应用依赖 |
| [框架选型矩阵](decisions/framework-selection-matrix.md) | 如何用同一业务接口和数据集比较候选方案 |
| [版本基线](version-baseline.md) | JDK、Spring Boot、AI 框架和协议 SDK 的锁定版本 |

## 运行与联调

| 文档 | 适用场景 |
|---|---|
| [本地直接启动](runbooks/local-toolchain.md) | 填写模型配置并从 IDE 启动三个应用 |
| [运行配置](runbooks/runtime-configuration.md) | 默认运行、完整 RAG 与公司基础设施怎样配置 |
| [RAG 本地准备](runbooks/rag-prerequisites.md) | 安装并手动启停 PostgreSQL/pgvector，建库、配置 Embedding 和恢复实验 |
| [知识导入与完整 RAG 联调](runbooks/knowledge-ingestion.md) | 准备 PostgreSQL/Provider，上传、发布、索引并验证最终回答 |

## 接口与数据

- [按专栏阶段阅读代码](reader-code-path.md)：完整仓库中每一阶段的最小阅读范围。
- [`contracts/`](../contracts/README.md)：OpenAPI 3.1、JSON Schema 和正反例样例。
- [`datasets/`](../datasets/README.md)：检索、模型、Agent 和安全 Golden Set。
- [`labs/`](../labs/README.md)：Spring AI Alibaba、LangChain4j、AgentScope 与 MCP/A2A 的隔离实验。
- [`apps/customer-web/`](../apps/customer-web/README.md)：客户咨询前端的运行和联调边界。
- [`deploy/`](../deploy/README.md)：部署平台必须提供的生产集成项。

## 验证档案

[`docs/reports/`](reports/README.md) 保存各个能力的测试范围、已验证行为和未覆盖条件。它们用于追溯当前实现的边界，不是上线许可；带时间、模型或代码版本的真实运行报告只能说明当次执行结果。

## 文档维护规则

- 架构或接口边界改变时，同步更新对应 ADR、OpenAPI 和回归测试。
- 命令同时考虑 macOS/Linux 与 Windows PowerShell；无法等价运行时明确限制。
- 不提交真实密钥、内网地址、用户数据或无法在公开仓库追溯的内部记录。
- 本地测试、外部集成和生产验收分别陈述，不使用“生产可用”代替具体的验证范围。

文档修改的提交方式与检查项见 [CONTRIBUTING.md](../CONTRIBUTING.md)。
