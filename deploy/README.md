# 生产部署集成边界

本目录记录部署平台需要接管的集成项。仓库目前不提供可直接用于任意公司环境的 Kubernetes、Helm 或 Terraform 清单；镜像仓库、域名、密钥、网络策略、可用区和运维系统都与目标环境有关，不应用虚构清单伪装已完成部署适配。

## 必须提供的外部能力

- PostgreSQL，Knowledge Service 需要 pgvector 与 `pg_trgm`；
- 符合当前配置的 Chat、Embedding 和可选 Rerank Provider；
- 可校验 issuer、audience、actor、scope、tenant 与 subject 的公司 IdP；
- 用于数据库密码、API Key、JWT 材料和客户端密钥的密钥管理系统；
- 生产对象存储、共享会话与限流实现；
- 网关、TLS、日志、Trace、Metric、告警和审计留存能力。

## 配置规则

各服务 `application.yml` 的 `production` 文档定义了数据库、身份、模型和下游服务配置路径。部署平台在不改变这些配置键的前提下注入环境差异和密钥。

API Key、数据库密码、JWT 材料和客户端密钥不能写入 Git、镜像层或测试报告。本地 `config/application.yml` 的明文配置只适用于个人开发机的短时测试。

## 上线前验证

部署完成后仍需独立验证：数据库迁移与恢复、ACL-before-TopK、JWT 与下游委托、Tool 幂等与未知结果对账、并发容量、告警和回滚。具体检查项见 [`docs/runbooks/release-checklist.md`](../docs/runbooks/release-checklist.md)。

Docker 不是日常开发前置条件，内存替身或本地 health 也不是生产基础设施验收。
