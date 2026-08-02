# 运行配置验证

## 已验证

- Knowledge Service、Ticket Agent Service 和 Customer BFF 的主资源目录各自只有一个 `application.yml`。
- 默认运行使用真实 Chat Provider、固定身份和跨服务 HTTP；会话、Agent 状态、审计和写 Tool 使用进程内实现。
- 仓库保留带占位 Key 的 `config/application.yml`，Knowledge Service 和 Ticket Agent Service 会自动读取。使用默认 OpenAI 配置时只需填写 API Key，运行后不能提交真实值。
- 将 `java-ai.knowledge.mode` 改为 `postgres-rag` 后，Knowledge Service 会启用 PostgreSQL/pgvector、Embedding、索引与检索；固定本地身份不要求 HTTP 请求携带 Token。
- PostgreSQL、JWT、Token Exchange 和下游地址都保留明确配置键，由对应 `mode` 选择适配器。密钥占位值必须由部署平台或密钥系统覆盖。
- Knowledge Service 和 Ticket Agent Service 只保留直接控制模型、数据库、JWT 和 Tool 的配置，不使用没有调用方的外部集成总开关。
- `test` Profile 只装配自动化测试配置；确定性模型不会进入默认运行路径，也不会伪造外部调用成功。
- 日常启动和测试可以直接使用 Maven Wrapper；Shell 和 PowerShell 只用于聚合多个构建边界或生成专项报告。

## 验证命令

```bash
node --test scripts/build-contract.test.mjs scripts/verification-scripts.test.mjs
./mvnw -pl services/knowledge-service,services/ticket-agent-service,apps/customer-bff \
  -Dtest=KnowledgeServiceDefaultStartupTest,TicketAgentDefaultStartupTest,CustomerBffDefaultStartupTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

填写 `config/application.yml` 中的 API Key 后执行测试：

```bash
./mvnw \
  -pl services/knowledge-service \
  -Dtest=LiveModelSmokeIT \
  -Dspring.config.additional-location=file:../../config/application.yml \
  -Djava-ai.smoke.report-path=target/live-model-smoke.md \
  test
```

Windows PowerShell：

```powershell
.\mvnw.cmd `
  -pl services/knowledge-service `
  -Dtest=LiveModelSmokeIT `
  -Dspring.config.additional-location=file:../../config/application.yml `
  -Djava-ai.smoke.report-path=target/live-model-smoke.md `
  test
```

## 外部验证边界

日常代码回归不访问模型和外部基础设施。仓库中的模型配置只保留不可用的占位 Key，生产密钥由部署平台覆盖。数据库、IdP、对象存储、业务 Tool、Windows 运行和容量结论仍需要目标环境测试；统一配置路径不会把本地结果扩大成生产验收。
