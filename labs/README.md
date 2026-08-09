# 框架与协议实验

`labs` 使用窄业务接口比较候选框架的行为，不是主应用的运行依赖。每个实验都保留框架原生 API，但通过本地业务端口隔离输入、错误和输出，避免框架类型泄漏到其他模块。

## 实验范围

- [Spring AI Alibaba](spring-ai-alibaba-lab/README.md)：DashScope 适配、检索替换指标、确认 Graph 和版本兼容决策。
- [LangChain4j](langchain4j-lab/README.md)：AI Services、租户范围 RAG、只读 Tool、结构化输出和框架路由。
- [AgentScope](agentscope-lab/README.md)：Tool 权限、人工介入、协作决策、MCP 工具和 A2A 任务状态。
- [MCP/A2A 互操作](protocol-interop-lab/README.md)：使用官方 SDK 验证协议交互与应用层信任边界。

## 怎样阅读这些实验

这些模块不是可独立启动的完整应用，不需要把它们逐个运行一遍。阅读时只跟随各模块 README 标出的入口类，对照相同业务接口下的适配、裁决和协议边界。主应用仍从 `KnowledgeServiceApplication`、`TicketAgentServiceApplication` 和 `CustomerBffApplication` 启动。

真实 Provider 或远程协议是否适合公司项目，要用自己的凭证、数据和目标环境另行判断；这里的代码只证明已写明的局部边界。
