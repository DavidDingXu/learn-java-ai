# Java AI 应用开发实战 · 完整目录

[返回课程首页](../../README.md)

课程已完结，共 52 篇全文。建议按模块顺序阅读；每篇文末可以跳转前后章节和对应实践。

<a id="module-01"></a>

## 01 · 系统边界与第一条调用

| 编号 | 文章全文 | 实践入口 |
| --- | --- | --- |
| 01 | [专栏介绍：Java 后端怎样系统学习 AI 应用开发落地](01-column-introduction.md) | [本阶段可运行源码](../../learning-stages/stage-01-system-boundaries/README.md) |
| 02 | [从一次客户咨询看系统分工：知识服务、工单 Agent 与 JDK8 老系统](02-system-context.md) | [本阶段可运行源码](../../learning-stages/stage-01-system-boundaries/README.md) |
| 03 | [Spring AI 为什么做主线，其他框架放在哪里](03-framework-selection.md) | [本阶段可运行源码](../../learning-stages/stage-01-system-boundaries/README.md) |
| 04 | [先跑通第一条真实调用：从 ChatClient 到可维护的垂直链路](04-first-model-call.md) | [本阶段可运行源码](../../learning-stages/stage-01-system-boundaries/README.md) |
| 05 | [多租户、身份和权限为什么不能相信请求参数](05-delegated-identity.md) | [本阶段可运行源码](../../learning-stages/stage-01-system-boundaries/README.md) |
| 06 | [JDK8 老系统不升级：先划清现代 AI 服务与老系统边界](06-jdk8-boundary.md) | [本阶段可运行源码](../../learning-stages/stage-01-system-boundaries/README.md) |

<a id="module-02"></a>

## 02 · 模型调用工程化

| 编号 | 文章全文 | 实践入口 |
| --- | --- | --- |
| 07 | [一次模型调用内部发生了什么：Message、Options、Token 与响应元数据](07-model-call-metadata.md) | [本阶段可运行源码](../../learning-stages/stage-02-model-engineering/README.md) |
| 08 | [Prompt 的工程边界：模板、版本、授权知识与提示注入](08-prompt-boundaries.md) | [本阶段可运行源码](../../learning-stages/stage-02-model-engineering/README.md) |
| 09 | [结构化输出为什么会失败：Schema、DTO 和业务校验](09-structured-output.md) | [本阶段可运行源码](../../learning-stages/stage-02-model-engineering/README.md) |
| 10 | [流式输出原理与实现：SSE、取消、心跳、断连和 TTFT](10-sse-streaming.md) | [本阶段可运行源码](../../learning-stages/stage-02-model-engineering/README.md) |
| 11 | [超时、并发保护、重试和降级：为什么不需要万能模型网关](11-model-resilience.md) | [本阶段可运行源码](../../learning-stages/stage-02-model-engineering/README.md) |
| 12 | [真实模型调用之后：怎样建立评测基线与可观测性](12-eval-observation.md) | [本阶段可运行源码](../../learning-stages/stage-02-model-engineering/README.md) |

<a id="module-03"></a>

## 03 · 企业 RAG

| 编号 | 文章全文 | 实践入口 |
| --- | --- | --- |
| 13 | [企业知识库先解决什么：文档、版本、状态和冲突](13-document-domain.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |
| 14 | [文档上传链路：对象存储、内容校验、隔离和解析失败](14-document-upload.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |
| 15 | [文档切分为什么影响召回：标题、条款、元数据和粒度](15-document-chunking.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |
| 16 | [向量检索原理与 pgvector 实现](16-pgvector-search.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |
| 17 | [权限为什么必须在 TopK 之前过滤](17-acl-before-topk.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |
| 18 | [混合检索、Query Rewrite、Rerank 什么时候有用](18-hybrid-retrieval.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |
| 19 | [RAG 回答为什么必须带证据：拒答、冲突、时效与知识注入](19-grounded-rag-answer.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |
| 20 | [增量索引为什么需要任务、租约和失败恢复](20-incremental-indexing.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |
| 21 | [RAG 质量怎样量化：Golden Set、Recall@K 和坏案例](21-rag-evaluation.md) | [本阶段可运行源码](../../learning-stages/stage-03-enterprise-rag/README.md) |

<a id="module-04"></a>

## 04 · 客户咨询与工单升级

| 编号 | 文章全文 | 实践入口 |
| --- | --- | --- |
| 22 | [C 端为什么需要薄 BFF：鉴权、限流、SSE 和 DTO 聚合](22-thin-bff.md) | [本阶段可运行源码](../../learning-stages/stage-04-customer-consultation/README.md) |
| 23 | [多轮会话原理：短期状态、裁剪、总结和过期](23-conversation-state.md) | [本阶段可运行源码](../../learning-stages/stage-04-customer-consultation/README.md) |
| 24 | [用户怎样判断回答可信：证据、反馈、拒答和重试](24-answer-trust.md) | [本阶段可运行源码](../../learning-stages/stage-04-customer-consultation/README.md) |
| 25 | [机器人解决不了时怎样平滑升级工单](25-ticket-handoff.md) | [本阶段可运行源码](../../learning-stages/stage-04-customer-consultation/README.md) |

<a id="module-05"></a>

## 05 · 受控 Agent 与老系统接入

| 编号 | 文章全文 | 实践入口 |
| --- | --- | --- |
| 26 | [Agent 与普通聊天有什么区别：目标、状态和受控执行](26-agent-boundary.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |
| 27 | [Tool Calling 原理与查询 Tool：最小权限、最小参数和最小返回](27-query-tools.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |
| 28 | [为什么模型只能选择工具：参数污染、结果污染和业务规则边界](28-tool-trust-boundaries.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |
| 29 | [写操作为什么必须先做风险分级](29-action-risk.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |
| 30 | [人工确认为什么不能只传一个字符串 token](30-approval-receipt.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |
| 31 | [幂等、并发和审计怎样跨越本地与远程事务边界](31-action-consistency.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |
| 32 | [JDK8 老系统接入 AI：OpenAPI、Java 8 客户端、认证、超时与错误处理](32-jdk8-integration.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |
| 33 | [串联工单 Agent 业务链：知识查询、人工确认与 JDK8 执行](33-ticket-agent-case.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |
| 34 | [Agent 质量怎样评测：工具路径、参数、拒绝和副作用](34-agent-evaluation.md) | [本阶段可运行源码](../../learning-stages/stage-05-controlled-agent/README.md) |

<a id="module-06"></a>

## 06 · 评测、安全与上线治理

| 编号 | 文章全文 | 实践入口 |
| --- | --- | --- |
| 35 | [AI 安全怎样持续回归：Injection、PII、ACL 与 Tool 越权](35-ai-security-regression.md) | [本阶段可运行源码](../../learning-stages/stage-06-production-readiness/README.md) |
| 36 | [观测做到什么程度才够用：Trace、Correlation、Metric、日志和成本](36-observability.md) | [本阶段可运行源码](../../learning-stages/stage-06-production-readiness/README.md) |
| 37 | [缓存、熔断、限流和降级：只保留业务真正需要的部分](37-production-resilience.md) | [本阶段可运行源码](../../learning-stages/stage-06-production-readiness/README.md) |
| 38 | [项目配置实战：让默认配置跑通，再按需替换基础设施](38-runtime-configuration.md) | [本阶段可运行源码](../../learning-stages/stage-06-production-readiness/README.md) |
| 39 | [上线前最后核对什么：测试、Eval、密钥配置、发布和回滚](39-release-checklist.md) | [本阶段可运行源码](../../learning-stages/stage-06-production-readiness/README.md) |

<a id="module-07"></a>

## 07 · 框架与协议迁移

| 编号 | 文章全文 | 实践入口 |
| --- | --- | --- |
| 40 | [DashScope Provider 适配实验：配置隔离、响应差异和回归边界](40-dashscope-provider-replacement.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 41 | [Embedding 与 Rerank 替换实验：怎样设计同集评测](41-domestic-embedding-rerank.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 42 | [Graph 人工确认路由实验：何时比现有状态机更合适](42-graph-confirmation-flow.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 43 | [Spring AI Alibaba 兼容性实验：依赖隔离、迁移 ADR 和回归成本](43-spring-ai-alibaba-compatibility.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 44 | [LangChain4j AI Services 适配实验：迁移一个低风险用例](44-langchain4j-ai-services.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 45 | [LangChain4j RAG 适配实验：复用现有业务契约](45-langchain4j-rag-contract.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 46 | [LangChain4j Tool 实验：只读调用与结构化输出](46-langchain4j-tool-structured-output.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 47 | [Spring AI 与 LangChain4j 共存实验：按业务能力选择框架](47-spring-ai-langchain4j-coexistence.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 48 | [AgentScope Tool 裁决实验：注册、身份传递与权限映射](48-agentscope-runtime-contracts.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 49 | [多 Agent 实际协作：并行取证、结果汇总与失败转人工](49-multi-agent-human-intervention.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 50 | [MCP 互操作实验：发现、准入、调用与远程失败](50-mcp-enterprise-tools.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 51 | [A2A 互操作实验：任务映射、幂等与状态边界](51-a2a-task-collaboration.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
| 52 | [结束语：把 Java + AI 能力迁移到真实公司项目](52-column-conclusion.md) | [本阶段可运行源码](../../learning-stages/stage-07-framework-boundaries/README.md) |
