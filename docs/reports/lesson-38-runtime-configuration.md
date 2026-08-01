# 运行配置验证

## 已验证

- Knowledge Service、Ticket Agent Service 和 Customer BFF 的主资源目录各自只有一个 `application.yml`。
- 默认 `demo` 可以在没有数据库、身份平台、模型或下游服务的情况下启动，被关闭的外部能力会明确返回不可用。
- 仓库保留带占位 Key 的 `config/application.yml`；真实模型专项测试直接读取它。使用默认 OpenAI 配置时只需填写 API Key，运行后不能提交真实值。
- PostgreSQL/pgvector、JWT、Token Exchange 和下游地址的生产配置结构直接保留在各服务 `application.yml` 的 `production` 文档中。其中的密钥占位值必须由部署平台或密钥系统覆盖。
- 三个应用的启动入口都会在创建业务 Bean 前检查生产必填项，拒绝空值、仓库占位值、非法 URL、错误数据库协议和冲突的 JWT 验签来源。
- Knowledge Service 和 Ticket Agent Service 只保留直接控制模型、数据库、JWT 和 Tool 的配置，不使用没有调用方的外部集成总开关。
- 默认 `demo` 使用禁用模型和进程内状态；`test` Profile 另外装配本地测试配置，两者都不会伪造外部调用成功。
- 日常启动和测试可以直接使用 Maven Wrapper；Shell 和 PowerShell 只用于聚合多个构建边界或生成专项报告。

## 验证命令

```bash
node --test scripts/build-contract.test.mjs scripts/verification-scripts.test.mjs
./mvnw -pl services/knowledge-service,services/ticket-agent-service,apps/customer-bff \
  -Dtest=ProductionConfigurationValidatorTest test
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
