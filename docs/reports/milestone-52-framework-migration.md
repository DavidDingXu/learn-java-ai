# 框架迁移与协议互操作

状态：已完成本地框架与协议边界验证（`VERIFIED_LOCAL_FRAMEWORK_BOUNDARIES`）

## 验证范围

- 正式服务继续使用 Spring AI 2.0 主线。
- Spring AI Alibaba、LangChain4j、AgentScope 和协议互操保留在独立 labs Maven 构建中，不把实验依赖带入正式服务。
- labs 内的端口保留主线业务语义，不引用服务内部类；共用评测规则比较 Provider、检索、Graph、AI Services、Tool、Runtime、MCP 和 A2A 边界。
- MCP Java SDK `2.0.0` 与 A2A Java SDK `1.1.0.Final` 执行了本地协议互操，不是只构造本地 Schema 对象。
- Ticket Agent 通过 PostgreSQL/Flyway 持久化任务、审计事件和确认决定；内存适配器只用于测试。
- 数据门禁包含 50 条检索案例、30 条 Agent 案例和 30 条合成安全案例。
- ADR 0003 记录框架实验进入正式服务的条件和回滚方式。

## 验证命令

```bash
./mvnw -f labs/pom.xml verify
bash scripts/release-gate.sh
```

## 适用范围

本地测试验证了框架 API 使用、依赖隔离、MCP/A2A 客户端互操和业务边界。实时 DashScope 调用、生产 MCP/A2A 认证、目标 PostgreSQL 容量和公司基础设施仍需要凭证与目标环境证据，不能由本地测试代替。
