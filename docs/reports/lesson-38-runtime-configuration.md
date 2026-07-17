# Lesson 38 Runtime Configuration Evidence

Status: VERIFIED_SINGLE_RUNTIME_CONFIGURATION

## Verified

- Knowledge Service、Ticket Agent Service 和 Customer BFF 的主资源目录各自只有一个 `application.yml`。
- 三个服务使用同一个根目录 `.env` 参数清单，不为开发、联调或 CI 维护额外运行 Profile。
- 模型、PostgreSQL/pgvector、JWT、Token Exchange 和下游地址通过 `JAVA_AI_*` 参数注入。
- 禁用模型、内存状态和关闭外部连接只存在于 `src/test/resources/application-test.yml`。
- Shell 与 PowerShell 使用同一 `verify-unit`、Smoke、Eval 和 release gate 入口。

## Verification

```bash
node --test scripts/build-contract.test.mjs scripts/verification-scripts.test.mjs
bash scripts/verify-unit.sh
```

## External Boundary

代码回归不访问模型和外部基础设施。数据库、IdP、对象存储、业务 Tool、Windows 运行和容量结论仍需要目标环境测试；单一配置只消除装配分叉，不会把本地结果扩大成生产验收。
