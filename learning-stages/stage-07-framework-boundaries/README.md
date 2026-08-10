# 阶段 07：框架与协议边界

阶段 06 的主业务链保持不变，本目录只增加四个隔离实验，不再复制一份完整主项目：

- `spring-ai-alibaba-lab`：Provider、Embedding/Rerank、Graph 和版本兼容边界
- `langchain4j-lab`：AI Services、RAG、Tool 与共存路由
- `agentscope-lab`：Tool 权限、协作决策、MCP 注册和 A2A 任务状态
- `protocol-interop-lab`：真实 MCP Java SDK 与 A2A Java SDK 客户端边界

主线继续使用 Spring AI `2.0.x`。Spring AI Alibaba 实验保持在它自己的兼容线，因此必须作为隔离模块评估，不能把它的 BOM 混入主线。

用 IDEA 打开本阶段根 `pom.xml`，所有在线实验继续读取项目根目录唯一的 `config/application-default.yml`。DashScope 专属字段已经预留在同一份模板中，只在运行第 40、41 篇时填写。

| 篇目 | 直接运行的主类 | 观察结果 |
|---|---|---|
| 40 | `SpringAiAlibabaLabApplication` | DashScope 回答、Token、耗时和响应 ID |
| 41 | `DashScopeRetrievalLabApplication` | 在线 Embedding/Rerank 排名与检索指标 |
| 42 | `ConfirmationGraphLabApplication` | 高低风险分支和确认状态流转 |
| 43 | `FrameworkCompatibilityLabApplication` | 版本兼容与模块隔离决策 |
| 44 | `LangChain4jLabApplication` | AI Services 的真实模型回答 |
| 45 | `LangChain4jRagLabApplication` | 租户范围上下文和引用 |
| 46 | `LangChain4jToolLabApplication` | 只读 Tool 与结构化决策 |
| 47 | 依次运行第 44-46 篇的三个主类 | 两套适配器独立运行，业务端口不泄漏框架类型 |
| 48 | `AgentScopeLabApplication` | 单 Agent 的模型、Tool 和权限链路 |
| 49 | `MultiAgentCollaborationApplication` | 两个专家并行取证并汇总；失败样例运行 `MultiAgentFailureApplication` |
| 50 | `McpLabApplication` | MCP 服务发现和工具调用 |
| 51 | `A2aLabApplication` | A2A Agent Card、任务发送和结果读取 |
