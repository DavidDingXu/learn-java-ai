# 维护者发布检查

本页仅服务于仓库维护者和发布流水线，不属于 Java 应用的本地启动步骤。

## 直接验证

运行 Java 21 主 reactor：

```bash
./mvnw verify
```

框架实验使用 `./mvnw -f labs/pom.xml verify`。Java 8 客户端使用独立 POM，并由 IDE 或 Maven 选择完整 JDK 8。这些命令都不需要项目专用环境变量。

`scripts/release-gate.sh` 与 `scripts/release-gate.ps1` 是可选聚合入口，会执行主 reactor、独立 labs、Java 8 客户端、前端、Node 契约与已跟踪/未跟踪文件的密钥扫描。它们不是启动 Java 服务的前置条件。

## 外部环境证据

只有在目标环境完成对应验证，才能声明该环境具备发布条件。部署流水线需要提供精确的服务地址并保留外部 health 结果。通用 health 冒烟只能说明一个应用端点存活；模型、检索、Agent 和安全评测需要不同身份与数据集，必须分别运行。

## 运维检查

- 数据库与 Schema 迁移已明确负责人、执行窗口和回退方式。
- Feature Flag 的默认状态与本次发布预期一致。
- Chat、Embedding 与 Rerank 模型名称和版本明确。
- 密钥来自公司批准的密钥系统。
- Agent 写 Tool 具备持久化幂等和未知结果恢复。
- 仪表盘和告警覆盖延迟、错误、Token、容量和不确定执行。
- 回滚方案分别处理应用、Prompt/模型与数据迁移。
- Runbook 记录值班所有者，并链接本次决策使用的精确报告。
