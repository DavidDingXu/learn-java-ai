# 阶段 07：框架与协议边界

阶段 06 的主业务链保持不变，本目录只增加四个隔离实验，不再复制一份完整主项目：

- `spring-ai-alibaba-lab`：Provider、Embedding/Rerank、Graph 和版本兼容边界
- `langchain4j-lab`：AI Services、RAG、Tool 与共存路由
- `agentscope-lab`：Tool 权限、协作决策、MCP 注册和 A2A 任务状态
- `protocol-interop-lab`：真实 MCP Java SDK 与 A2A Java SDK 客户端边界

主线继续使用 Spring AI `2.0.x`。Spring AI Alibaba 实验保持在它自己的兼容线，因此必须作为隔离模块评估，不能把它的 BOM 混入主线。

用 IDEA 打开本阶段根 `pom.xml`，每篇文章再进入对应实验。所有实验继续读取项目根目录唯一的 `config/application-default.yml`；DashScope 专属字段也追加在这一个文件中。从 IDEA 运行 `SpringAiAlibabaLabApplication`、`LangChain4jLabApplication` 或 `AgentScopeLabApplication`；MCP 与 A2A 分别运行 `McpLabApplication` 和 `A2aLabApplication`。
