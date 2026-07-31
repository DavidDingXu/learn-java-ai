# 里程碑 06：身份与接口边界

状态：已完成本地边界验证（`VERIFIED_LOCAL_BOUNDARIES`）

项目提供可执行的 JWT 委托规则、HTTP/OpenAPI 契约、JSON Schema 样例和独立编译的 JDK8 DTO 边界。

以下检查需要在同一份代码上通过：

```bash
node --test scripts/lesson-05-06-contract.test.mjs
./mvnw -pl services/knowledge-service,apps/customer-bff,quality/eval-runner verify
./mvnw -f integrations/jdk8-client/pom.xml verify
```

## 已验证的行为

- 面向 `customer-bff` 的客户令牌不能直接调用 Knowledge Service。
- 委托 JWT 的目标受众必须是 `knowledge-service`，同时保留 subject 和 tenant，将 `customer-bff` 记为 actor，并携带 `knowledge:answer` scope。
- 公开请求的 JSON 不接受身份和授权事实。
- 老系统接口 DTO 可以由 JDK8 独立编译和运行。

## 适用范围

这些检查验证本地身份与接口边界。生产身份平台接入、真实密钥轮换和外部老系统部署仍需在目标环境验收。
