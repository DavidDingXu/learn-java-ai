# 按专栏阶段阅读代码

仓库保存一份完整业务实现，同时在 [`learning-stages`](../learning-stages/README.md) 中提供七个可直接运行的学习切片。先运行当前阶段的 `Application`，看到可观察结果后，再按下面的入口进入最终实现。

## 01-12：模型调用

先运行 `KnowledgeServiceApplication`，再按调用方向阅读：

1. `KnowledgeAnswerController`：HTTP 输入输出；
2. `KnowledgeAnswerService`：回答用例；
3. `KnowledgeAnswerModel`：业务层需要的模型能力；
4. `SpringAiKnowledgeAnswerModel`：Spring AI 适配；
5. `KnowledgeAnswerValidator`：模型输出进入业务响应前的校验。

这一阶段先忽略 `document`、`indexing` 和 `retrieval` 包。默认 classpath 知识已经能支撑第一条真实调用。

## 13-21：企业 RAG

仍从 `KnowledgeServiceApplication` 启动，把 `java-ai.knowledge.mode` 切换为 `postgres-rag` 后，按数据流阅读：

1. `KnowledgeDocumentController`：上传与发布；
2. `KnowledgeDocument`、`DocumentVersion`：文档业务状态；
3. `PolicyDocumentChunker`：文档切分；
4. `IndexTaskWorker`、`DocumentVersionIndexingService`：索引任务；
5. `PgVectorKnowledgeChunkIndexSink`：向量写入与版本切换；
6. `HybridKnowledgeRetrievalService`：检索编排；
7. `RetrievalPolicyContextSource`：检索结果进入回答上下文；
8. `RetrievalMetricsCalculator`：检索质量计算。

连续操作步骤见[知识文档导入](runbooks/knowledge-ingestion.md)。

## 22-25：客户咨询

运行三个后端应用，再从 Customer BFF 进入：

1. `CustomerConsultationController`：渠道接口；
2. `CustomerConsultationService`：会话、回答与升级；
3. `ConsultationSession`：短期会话状态；
4. `KnowledgeAnswerStreamClient`：知识服务 SSE 边界；
5. `TicketTaskClient`：工单升级边界。

前端只需要看 `apps/customer-web/src` 中的页面状态与 API 适配，不要从前端反推租户和权限规则。

## 26-34：受控工单 Agent

从 `TicketAgentServiceApplication` 进入，按控制权归属阅读：

1. `AgentTaskController`：任务入口；
2. `TicketAgentOrchestrator`：有限步循环；
3. `BusinessToolCatalog`：服务端 Tool 目录与参数编译；
4. `ToolConfirmationService`：人工确认；
5. `HttpLegacyWriteToolExecutor`：远程写入与结果分类；
6. `AgentTask`：状态与并发边界。

模型只提出候选步骤。权限、风险、确认、幂等和最终状态都在这些 Java 类型中。

## 35-39：上线治理

这一阶段不新增一条独立业务链，重点回看已有入口怎样接入安全、观测、容量和配置：

- `SecurityConfiguration` 与各身份工厂；
- `MicrometerAgentTelemetry`；
- `SemaphoreAgentRunAdmission`；
- 三个应用的 `application.yml`；
- `docs/runbooks` 中的运行和生产替换边界。

## 40-51：框架与协议边界实验

`labs` 不是一组需要逐个启动的应用。每篇只看对应入口：

| 篇目 | 阅读入口 |
|---|---|
| 40-43 | `DashScopeProviderAdapter`、`RetrievalReplacementExperiment`、`ConfirmationGraph`、`FrameworkCompatibilityDecision` |
| 44-47 | `LangChain4jPolicyAnswerAdapter`、`TenantScopedRagAdapter`、`LangChain4jTicketDecisionAdapter`、`FrameworkCoexistencePolicy` |
| 48-49 | `AgentScopeTicketRuntime`、`CollaborationPolicy` |
| 50-51 | `EnterpriseMcpClient`、`EnterpriseA2aClient`、`A2aTaskCoordinator` |

这些入口回答的是“适配边界是否成立”，不是“一个新的完整业务系统已经跑通”。主应用仍由前三个 `Application` 类启动。
