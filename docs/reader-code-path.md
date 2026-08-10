# 按专栏阶段阅读代码

仓库保存一份完整业务实现，同时在 [`learning-stages`](../learning-stages/README.md) 中提供七个可直接运行的学习切片。先运行当前阶段的 `Application`，看到可观察结果后，再按下面的入口进入最终实现。

## 先把搜索范围缩到当前阶段

不要把仓库根目录和七个阶段同时作为一个 IDEA 项目全局搜索。每到一个阶段，新开窗口并导入该阶段的根 `pom.xml`，`Find in Files` 和 `Search Everywhere` 使用 `Project Files` 范围。这样只会索引当前阶段自己的增量，以及它明确复用的前序模块。

每个阶段 README 都列出了本阶段新增能力和启动入口。先从对应的 `Application`、阶段 HTTP 文件和下文列出的 Controller、应用服务或 Runner 进入；其余包暂时不读。需要对照最终实现时，再单独打开仓库根项目。七个阶段用于学习增量，根目录模块是部署和继续开发的完整实现，两者不要在同一个搜索结果里混读。

所有阶段共享项目根目录唯一的 `config/application-default.yml`。阶段目录中的 `config/application-base.yml` 只是不可编辑的公共默认值，不复制 API Key，也不为本地、远程各建一套配置。

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

连续操作步骤见 [RAG 本地准备](runbooks/rag-prerequisites.md)和[知识文档导入](runbooks/knowledge-ingestion.md)。

## 22-25：客户咨询

运行三个后端应用，再从 Customer BFF 进入：

1. `CustomerConsultationController`：渠道接口；
2. `CustomerConsultationService`：会话、回答与升级；
3. `ConsultationSession`：短期会话状态；
4. `KnowledgeAnswerStreamClient`：知识服务 SSE 边界；
5. `TicketTaskClient`：工单升级边界。

前端从 `learning-stages/stage-04-customer-consultation/apps/customer-web/src` 阅读页面状态与 API 适配，不要从前端反推租户和权限规则。

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

`labs` 不是一个需要同时启动所有模块的组合应用。读到一组实验时，只运行对应的 `main`，再查看它调用的适配器：

| 篇目 | 运行入口 | 重点代码 |
|---|---|---|
| 40 | `SpringAiAlibabaLabApplication` | `DashScopeProviderAdapter` |
| 41 | `DashScopeRetrievalLabApplication` | `OnlineRetrievalReplacementExperiment` |
| 42-43 | `ConfirmationGraphLabApplication`、`FrameworkCompatibilityLabApplication` | `ConfirmationGraph`、`FrameworkCompatibilityDecision` |
| 44-47 | `LangChain4jLabApplication`、`LangChain4jRagLabApplication`、`LangChain4jToolLabApplication` | `LangChain4jPolicyAnswerAdapter`、`TenantScopedRagAdapter`、`LangChain4jTicketDecisionAdapter`、`FrameworkCoexistencePolicy` |
| 48 | `AgentScopeLabApplication` | `AgentScopeTicketRuntime` |
| 49 | `MultiAgentCollaborationApplication` | `CollaborationPolicy`、`MultiAgentCoordinator` |
| 50 | `McpLabApplication` | `EnterpriseMcpClient` |
| 51 | `A2aLabApplication` | `EnterpriseA2aClient`、`A2aTaskCoordinator` |

这些入口回答的是“适配边界是否成立”，不是“一个新的完整业务系统已经跑通”。主应用仍由前三个 `Application` 类启动。
