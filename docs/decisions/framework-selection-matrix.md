# Framework Selection Matrix

## Decision Context

主线团队假设是已有 Spring Boot 研发、测试、安全和可观测体系的 Java 后端团队。比较对象必须在同一业务合同、同一数据集和同一验收口径下评估；API 写法更短或示例更多，不足以改变主线。

| Candidate | Spring team fit | Chat/Output/RAG/Tool/MCP | Agent runtime | Security/Observation integration | Provider reach | Dependency risk | Exit cost | Decision |
|---|---|---|---|---|---|---|---|---|
| Spring AI | 高：沿用 Spring Boot 配置、DI、测试和 Actuator | 主线能力覆盖完整 | 提供模型、Advisor、Tool 和 MCP 基础，不把它当完整 Agent runtime | 高：可直接接 Spring Security、Micrometer 和 OpenTelemetry | 多 Provider 抽象，特有能力可能滞后 | 中：需锁定 Boot/AI/Provider 兼容组合 | 中：通过业务端口隔离后可控 | Java 21 主线 |
| Spring AI Alibaba | 高：建立在 Spring AI 之上 | 继承 Spring AI，并强化通义/DashScope 生态 | 强：官方提供 ReactAgent、Graph 和多 Agent 编排 | 中高：仍处于 Spring 体系，但扩展运行时需单独验证 | 国内阿里云生态更直接 | 中高：同时受 Spring AI 与 Alibaba 版本矩阵影响 | 中高 | 放入独立 lab，在 Agent 专题按同一合同验证 |
| LangChain4j | 中高：有 Spring Boot starter 和声明式 AI Services | Chat、结构化输出、RAG、Tool、MCP 均有独立抽象 | 中：可组合 AI Services 和工具，但运行时取舍与主线不同 | 中：需额外对齐现有安全和观测规范 | Provider 覆盖广 | 中：引入另一套核心抽象 | 中高 | 放入独立 lab，做 RAG/Tool 迁移对照 |
| AgentScope | 中：Java 可用，但不是 Spring 应用框架的自然延伸 | 模型、Tool、MCP 以 Agent 用例为中心 | 强：Agent、Tool、工作流和多 Agent 是核心 | 中：需验证与现有 Spring Security、Trace 体系的接缝 | 取决于模型扩展 | 中高：Agent runtime 会影响应用结构 | 高 | 放入独立 lab，用于复杂 Agent runtime 专题 |
| Provider SDK | 低：只解决供应商 API 调用 | Chat 通常最及时；RAG、Tool 治理、MCP 和观测要自行拼装 | 无统一运行时 | 低：认证、重试、指标和审计需自行接入 | 单一 Provider 最深 | 高：业务代码容易绑定供应商协议 | 高 | 仅在 Spring AI 缺失关键能力时放入窄适配器 |
| Existing company AI platform | 取决于公司平台合同 | 取决于平台已治理能力 | 取决于平台 | 若已统一身份、配额、审计和观测则很高 | 由公司平台决定 | 取决于平台 SLA 和版本治理 | 取决于合同稳定性 | 公司已有强制平台时，平台合同优先于本仓库 Provider adapter |

## Why Spring AI Is Mainline

选择 Spring AI 不是因为它在每个维度都最强，而是它对目标团队的综合迁移成本最低：模型调用可以留在基础设施适配器，业务层继续使用 Spring 团队熟悉的依赖注入、安全、配置、测试和可观测方式。主线仍要通过业务端口控制退出成本，不能让 Controller、领域对象或跨服务合同暴露 Spring AI 类型。

Spring AI Alibaba、LangChain4j 和 AgentScope 都有明确价值，但价值点不同。把它们同时装进主 reactor，只会制造依赖冲突和多套运行时语义，无法形成可靠比较。独立 labs 以相同业务合同和数据集验证后，差异才能落到代码量、行为、Trace、评测结果和维护成本。

## Replacement Conditions

满足以下任一条件时重新评审主线：

- 必需的 Provider 能力无法通过 Spring AI 或窄适配器实现。
- 公司已有强制的 AI 平台合同，且它提供身份、配额、审计、路由和 SLA。
- 其他框架在同一业务合同、同一数据集上取得可重复的质量或维护成本优势。
- 当前 Spring Boot、Spring AI 或 Provider 组合不再满足安全和兼容支持要求。

更换主线必须新增 ADR，并提供依赖树、回归结果、迁移影响和回滚路径。

## Official References

- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba Overview](https://java2ai.com/docs/overview/)
- [LangChain4j AI Services](https://docs.langchain4j.dev/tutorials/ai-services/)
- [LangChain4j MCP](https://docs.langchain4j.dev/tutorials/mcp/)
- [AgentScope Java](https://java.agentscope.io/v2/en/intro.html)
