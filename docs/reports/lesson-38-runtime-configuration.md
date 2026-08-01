# Runtime Configuration Verification

Status: VERIFIED_SINGLE_RUNTIME_CONFIGURATION

## Verified

- Knowledge Service、Ticket Agent Service 和 Customer BFF 的主资源目录各自只有一个 `application.yml`。
- 默认 `demo` 可以在没有数据库、身份平台、模型或下游服务的情况下启动，被关闭的外部能力会明确返回不可用。
- 仓库保留 `config/application.example.yml`；真实模型专项测试读取 Git 忽略的 `config/application.yml`。使用默认 OpenAI 配置时只需填写 API Key。
- PostgreSQL/pgvector、JWT、Token Exchange 和下游地址的生产配置结构直接保留在各服务 `application.yml` 的 `production` 文档中。其中的密钥占位值必须由部署平台或密钥系统覆盖。
- 禁用模型、内存状态和关闭外部连接只存在于 `src/test/resources/application-test.yml`。
- 日常启动和测试可以直接使用 Maven Wrapper；Shell 和 PowerShell 只用于聚合多个构建边界或生成专项报告。

## Verification

```bash
node --test scripts/build-contract.test.mjs scripts/verification-scripts.test.mjs
./mvnw -pl services/knowledge-service,services/ticket-agent-service,apps/customer-bff test
```

先把示例复制为本地配置，填写 API Key 后再执行测试：

```bash
cp config/application.example.yml config/application.yml
```

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

## External Boundary

日常代码回归不访问模型和外部基础设施。本地模型配置已被 Git 忽略，生产密钥则由部署平台覆盖。数据库、IdP、对象存储、业务 Tool、Windows 运行和容量结论仍需要目标环境测试；统一配置路径不会把本地结果扩大成生产验收。
