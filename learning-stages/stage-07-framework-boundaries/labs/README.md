# 框架与协议实验

`labs` 使用窄业务接口比较候选框架的行为，不是主应用的运行依赖。每个实验都保留框架原生 API，但通过本地业务端口隔离输入、错误和输出，避免框架类型泄漏到其他模块。

## 实验范围

- [Spring AI Alibaba](spring-ai-alibaba-lab/README.md)：DashScope 适配、检索替换指标、确认 Graph 和版本兼容决策。
- [LangChain4j](langchain4j-lab/README.md)：AI Services、租户范围 RAG、只读 Tool、结构化输出和框架路由。
- [AgentScope](agentscope-lab/README.md)：Tool 权限、人工介入、协作决策、MCP 工具和 A2A 任务状态。
- [MCP/A2A 互操作](protocol-interop-lab/README.md)：使用官方 SDK 验证协议交互与应用层信任边界。

## 直接运行

用 IDEA 把 `labs/pom.xml` 作为独立项目打开，再按文章运行对应主类：

- `SpringAiAlibabaLabApplication`、`DashScopeRetrievalLabApplication`、`ConfirmationGraphLabApplication`、`FrameworkCompatibilityLabApplication`：分别对应第 40-43 篇。
- `LangChain4jLabApplication`、`LangChain4jRagLabApplication`、`LangChain4jToolLabApplication`：分别对应第 44-46 篇。
- `AgentScopeLabApplication`：单 Agent 的真实模型与只读 Tool；`MultiAgentCollaborationApplication`：两个专家并行取证并由第三个 Agent 汇总。
- `MultiAgentFailureApplication`：无需模型即可观察专家失败后停止汇总、转人工的路径。
- `McpLabApplication`：本地启动真实 MCP Server，并由官方客户端完成发现和调用。
- `A2aLabApplication`：本地启动 A2A 服务，并由官方客户端发现 Agent Card、发送任务和读取结果。

需要模型的实验继续读取项目根目录唯一的 `config/application-default.yml`。LangChain4j 与 AgentScope 直接复用其中的 OpenAI 兼容配置；Spring AI Alibaba 的在线实验填写模板中预留的 `lab.dashscope`。各模块 `src/main/resources` 中的文件仅保存版本化默认值，不需要修改。

## 每篇实验实际运行到哪里

| 篇目 | 运行入口 | 读者会看到 | 评估边界 |
|---|---|---|---|
| 40 | `SpringAiAlibabaLabApplication` | DashScope 回答、Token、耗时、响应 ID | 换真实语料后再判断质量和配额 |
| 41 | `DashScopeRetrievalLabApplication` | 在线 Embedding/Rerank 排名、Recall、MRR、耗时 | 换目标语料后再决定是否重建索引 |
| 42 | `ConfirmationGraphLabApplication` | Graph 的直达、暂停和恢复 | 持久化与人工系统仍由业务接入 |
| 43 | `FrameworkCompatibilityLabApplication` | 依赖线的隔离决策 | 两条框架线保持独立模块 |
| 44 | `LangChain4jLabApplication` | 真实 Chat API 回答 | 只迁移业务端口，不迁移整个主应用 |
| 45 | `LangChain4jRagLabApplication` | 真实 Chat API、受控租户语料和引用 | pgvector ACL 仍由主线 RAG 实现 |
| 46 | `LangChain4jToolLabApplication` | 真实 Chat API、结构化输出和只读 Tool | 写动作继续经过确认边界 |
| 47 | 三个入口加共存策略 | 同一业务端口的两种适配 | 两套框架无需塞进同一进程 |
| 48 | `AgentScopeLabApplication` | ReAct Agent 调用获准的只读 Tool | 业务状态与权限仍由应用层掌握 |
| 49 | `MultiAgentCollaborationApplication` | 两个专家并行取证、第三个 Agent 汇总 | 失败路径由协调器转人工 |
| 50 | `McpLabApplication` | 本地 MCP Server 与官方客户端互操作 | 远程接入还要补企业鉴权和容量评估 |
| 51 | `A2aLabApplication` | 本地 A2A 服务发现、任务发送与结果读取 | 跨组织接入还要协商身份与交付语义 |

这些入口让读者先看到真实行为，再进入代码理解边界。迁移时再用自己的语料、权限、配额和目标环境评估质量与稳定性。
