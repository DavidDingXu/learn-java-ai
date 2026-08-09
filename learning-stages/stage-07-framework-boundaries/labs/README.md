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

需要模型的实验先填写各模块的 `src/main/resources/application.yml` 或 `application.properties`。读者不需要执行单元测试，也不需要配置环境变量。主业务应用仍从 `KnowledgeServiceApplication`、`TicketAgentServiceApplication` 和 `CustomerBffApplication` 启动。

一次真实调用只能证明当前凭证、模型和样例链路跑通。迁移到公司项目时仍要用自己的语料、权限、配额和目标环境重新评估质量与稳定性。
