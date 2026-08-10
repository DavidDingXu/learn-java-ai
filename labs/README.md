# 框架与协议实验

`labs` 使用窄业务接口比较候选框架的行为，不是主应用的运行依赖。每个实验都保留框架原生 API，但通过本地业务端口隔离输入、错误和输出，避免框架类型泄漏到其他模块。

## 实验范围

- [Spring AI Alibaba](spring-ai-alibaba-lab/README.md)：DashScope 适配、检索替换指标、确认 Graph 和版本兼容决策。
- [LangChain4j](langchain4j-lab/README.md)：AI Services、租户范围 RAG、只读 Tool、结构化输出和框架路由。
- [AgentScope](agentscope-lab/README.md)：Tool 权限、人工介入、协作决策、MCP 工具和 A2A 任务状态。
- [MCP/A2A 互操作](protocol-interop-lab/README.md)：使用官方 SDK 验证协议交互与应用层信任边界。

## 直接运行

用 IDEA 把 `labs/pom.xml` 作为独立项目打开，再按文章运行对应主类：

- `SpringAiAlibabaLabApplication`：DashScope Provider、检索替换、Graph 或兼容性决策。
- `LangChain4jLabApplication`：真实 OpenAI 兼容 Provider 下的回答、RAG 或 Tool。
- `AgentScopeLabApplication`：真实模型驱动的 ReAct Agent 与只读 Tool。
- `McpLabApplication`：本地启动真实 MCP Server，并由官方客户端完成发现和调用。
- `A2aLabApplication`：本地启动 A2A 服务，并由官方客户端发现 Agent Card、发送任务和读取结果。

需要模型的实验继续读取项目根目录唯一的 `config/application-default.yml`。LangChain4j 与 AgentScope 直接复用其中的 OpenAI 兼容配置；Spring AI Alibaba 的 DashScope 实验在同一文件追加 `lab.dashscope` 与 `lab.mode`。各模块 `src/main/resources` 中的文件只是不可编辑的默认值。

## 每篇实验实际运行到哪里

| 篇目 | 运行方式 | 真实依赖 | 不能据此声称 |
|---|---|---|---|
| 40 | `SpringAiAlibabaLabApplication/provider` | DashScope Chat API | Provider 已满足公司语料质量和配额 |
| 41 | `retrieval` 固定 Golden Case | 无外部服务 | 已在线调用 Embedding 或 Rerank |
| 42 | `ConfirmationGraph` 本地状态图 | 真实 Graph 运行时 | 已接入工单持久化和人工系统 |
| 43 | 兼容性决策程序 | 当前依赖版本 | 两条技术线可以混入同一 Boot 应用 |
| 44 | LangChain4j `answer` | 真实 Chat API | 主业务已经迁移到 LangChain4j |
| 45 | LangChain4j `rag` | 真实 Chat API、受控内存检索 | 已执行 pgvector ACL SQL |
| 46 | LangChain4j `tool` | 真实 Chat API、本地只读 Tool | 写 Tool 可以绕过确认执行 |
| 47 | 三种模式加共存策略 | 真实 Chat API | 两套框架必须运行在同一进程 |
| 48 | AgentScope ReAct | 真实 Chat API、本地只读 Tool | 已实现完整工单 Agent |
| 49 | `CollaborationPolicy` | 无外部服务 | 多个 Agent 已经协同执行 |
| 50 | MCP 主类 | 本地真实 MCP Server 与官方客户端 | 远程企业 MCP 已完成鉴权与容量验证 |
| 51 | A2A 主类 | 本地真实 A2A 服务与官方客户端 | 跨组织 Agent 已完成生产互操作 |

一次真实调用只能证明当前凭证、模型和样例链路跑通。迁移到公司项目时仍要用自己的语料、权限、配额和目标环境重新评估质量与稳定性。
