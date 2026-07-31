# Milestone 06 Identity and Contract Boundaries

## Status

`VERIFIED_LOCAL_BOUNDARIES`

项目提供可执行的 JWT 委托规则、HTTP/OpenAPI 契约、JSON Schema 样例和独立编译的 JDK8 DTO 边界。

The milestone is satisfied when all of the following pass on one commit:

```bash
node --test scripts/lesson-05-06-contract.test.mjs
./mvnw -pl services/knowledge-service,apps/customer-bff,quality/eval-runner verify
./mvnw -f integrations/jdk8-client/pom.xml verify
```

## Accepted claims

- A customer token for `customer-bff` cannot be used directly against Knowledge Service.
- A delegated JWT must target `knowledge-service`, preserve subject and tenant, identify `customer-bff` as actor, and carry `knowledge:answer` scope.
- Public request contracts do not accept identity or authorization facts in JSON.
- The legacy contract DTO compiles and runs on JDK8.

## 适用范围

这些检查验证本地身份与接口边界。生产身份平台接入、真实密钥轮换和外部老系统部署仍需在目标环境验收。
