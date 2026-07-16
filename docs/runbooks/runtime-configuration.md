# Runtime Configuration

## One Configuration Model

Knowledge Service、Ticket Agent Service 和 Customer BFF 各自只保留一份主 `application.yml`。三个文件使用相同原则：

1. 非敏感默认值写在 YAML，例如端口、超时和连接池上限。
2. 模型密钥、数据库密码、身份参数和下游地址引用 `JAVA_AI_*` 环境变量。
3. 三个服务都通过 `spring.config.import` 读取项目根目录 `.env`。
4. 必填参数缺失时启动失败，不静默关闭真实适配器，也不返回固定业务答案。

## Prepare The Configuration

```bash
cp .env.example .env
```

按 `.env.example` 的分组填写模型、Knowledge 数据库、Ticket 数据库、身份平台和下游服务参数。`.env` 已被 `.gitignore` 排除，仍应限制本机文件权限；生产部署应由密钥系统和平台配置覆盖，不上传 `.env`。

检查关键参数是否仍为空：

```bash
rg -n '=$' .env
```

## Start The Services

从项目根目录分别启动，确保三个进程读取同一个 `.env`：

```bash
./mvnw -pl services/knowledge-service spring-boot:run
./mvnw -pl services/ticket-agent-service spring-boot:run
./mvnw -pl apps/customer-bff spring-boot:run
```

默认端口为 8081、8082 和 8080。先检查 health，再执行真实模型 Smoke、检索 Eval 或业务接口验证。health 只能确认进程和基础 Bean 已启动，不覆盖数据库查询、模型质量和端到端业务结果。

## Test Isolation

`src/test/resources/application-test.yml` 关闭模型和外部连接，并为需要状态的测试选择内存适配器。Spring Boot 上下文测试显式使用 `@ActiveProfiles("test")`；模型协议测试和真实模型 Smoke 通过测试属性覆盖必要参数。

这套测试配置不会打进生产 Jar，正常启动应用时也不需要切换 Profile。快速代码回归统一执行：

```bash
scripts/verify-unit.sh
```

## Deployment Injection

公司测试、预生产和生产部署使用同一组配置键，只替换值：

- 密钥由 Secret Manager、Vault 或平台 Secret 注入；
- PostgreSQL/pgvector 使用独立数据库或 Schema 与最小权限账号；
- JWT issuer、JWK Set URI 和 audience 与目标 IdP 对齐；
- Knowledge、Ticket 和 Legacy Tool 地址由服务发现或平台变量提供；
- 连接池、超时与并发上限依据容量测试调整。

CI 同样调用 `verify-unit` 或 `release-gate`，并按任务需要注入临时数据库和测试凭证。CI 是命令执行位置，不是另一套应用配置。

## Verification Scope

| Check | Command or source | Covered scope |
|---|---|---|
| Code regression | `scripts/verify-unit.sh` | Java/Node 接口检查、业务规则和构建边界 |
| Real API | model Smoke and Eval scripts | Provider 协议、模型响应和固定数据集结果 |
| Integration | deployed service URLs and scoped tokens | 数据库、身份与跨服务行为 |
| Production acceptance | capacity, SLO, migration and rollback reports | 目标环境是否满足上线条件 |

这些检查使用同一套配置键，但覆盖范围不同，不能用其中一项代替另一项。
