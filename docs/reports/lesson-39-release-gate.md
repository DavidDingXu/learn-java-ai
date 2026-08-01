# 发布门禁验证记录

状态：本地发布门禁已验证（`VERIFIED_LOCAL_RELEASE_GATE`）

## 已验证的行为

- macOS/Linux 与 Windows 发布脚本覆盖 Java 主工程、隔离 labs、JDK8 客户端、Customer Web、接口契约和敏感信息扫描。
- 日常本地验证不要求外部地址；只有部署流水线明确提供目标 URL 时，聚合门禁才运行外部健康检查。
- 模型、检索、Agent 和安全评测各自保留数据集、身份与报告，不合并成一个笼统总分。
- 发布文档覆盖数据库迁移、功能开关、指标、未知结果对账，以及应用、模型、索引和数据库回滚。

## 验证命令

```bash
./mvnw verify
./mvnw -f labs/pom.xml verify
./mvnw -f integrations/jdk8-client/pom.xml verify
npm --prefix apps/customer-web test
node --test scripts/*.test.mjs
```

需要一次聚合全部构建和敏感信息扫描时，运行 `scripts/release-gate.sh` 或 PowerShell 等价入口。外部健康检查只在部署环境显式提供目标地址时启用。

## 适用范围

本地门禁不能证明已部署模型、数据库、向量索引、对象存储或 Legacy Tool 已经可用。生产发布还需要保存目标环境的模型评测、数据迁移、容量、告警和回滚演练结果。
