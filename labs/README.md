# 框架与协议实验

`labs` 使用窄业务接口比较候选框架的行为，不是主应用的运行依赖。每个实验都保留框架原生 API，但通过本地业务端口隔离输入、错误和输出，避免框架类型泄漏到其他模块。

## 实验范围

- [Spring AI Alibaba](spring-ai-alibaba-lab/README.md)：DashScope 适配、检索替换指标、确认 Graph 和版本兼容决策。
- [LangChain4j](langchain4j-lab/README.md)：AI Services、租户范围 RAG、只读 Tool、结构化输出和框架路由。
- [AgentScope](agentscope-lab/README.md)：Tool 权限、人工介入、协作决策、MCP 工具和 A2A 任务状态。
- [MCP/A2A 互操作](protocol-interop-lab/README.md)：使用官方 SDK 验证协议交互与应用层信任边界。

## 运行

在项目根目录执行：

```bash
./mvnw -f labs/pom.xml verify
```

Windows PowerShell：

```powershell
.\mvnw.cmd -f labs/pom.xml verify
```

默认测试不需要 Docker 或真实模型密钥。真实 Provider 和远程协议验证应使用独立、受密钥控制的集成测试，不能替代确定性回归。
