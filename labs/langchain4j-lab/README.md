# LangChain4j 业务端口实验

这个模块验证选定用例能否迁移到 LangChain4j，而不重写主应用架构。

- `LangChain4jPolicyAnswerAdapter` 使用 AI Services 实现稳定的回答端口，并将调用失败与非法输出映射为应用错误。
- `TenantScopedRagAdapter` 在内容进入模型前，通过 `KnowledgeSearchPort` 执行带 ACL 的检索。
- `LangChain4jTicketDecisionAdapter` 执行只读 Tool，并将结果解析为业务记录。
- `FrameworkCoexistencePolicy` 按能力路由，不在模块之间传递框架对象。

在项目根目录执行：

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

实验验证的是端口迁移和错误边界，不代表已完成真实 Provider、性能和生产运维验收。
