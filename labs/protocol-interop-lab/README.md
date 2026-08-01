# MCP 与 A2A 互操作实验

这个模块使用 MCP Java SDK 与 A2A Java SDK 验证真实协议交互，同时保留企业应用必须的本地信任决策。

- MCP 实验通过 Streamable HTTP 完成初始化、工具发现和 `tools/call`。
- 远程工具必须通过本地允许目录、只读和非破坏性规则，不会因为 Server 已返回 Schema 就直接交给模型。
- A2A 实验验证任务创建、状态转移、重复请求和不确定交付结果。
- 协议负责互操作；身份、权限、超时、幂等、审计和错误映射仍是应用责任。

在项目根目录执行：

```bash
./mvnw -f labs/pom.xml -pl protocol-interop-lab test
```

默认测试在本机启动协议 Server，不访问外部服务。
