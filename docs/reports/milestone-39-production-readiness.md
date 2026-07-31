# 里程碑 39：生产工程基线

Status: VERIFIED_LOCAL_GATE_IMPLEMENTATION

## 已完成的能力

这一阶段加入版本化 Agent 安全案例、审计 PII 检测、低基数 Micrometer 指标、受管 Prometheus 端点、Agent Run 并发限制、稳定 429 错误，以及跨平台发布门禁。

## 验证范围

门禁覆盖 Java 21 主工程、隔离框架 labs、独立 JDK8 客户端、Customer Web、Node 契约检查，以及已跟踪和未忽略文件的敏感信息扫描。具体命令与边界见 `lesson-39-release-gate.md`。

## 适用范围

这套基线提供可执行门禁，不代表生产容量已经验收。公司环境仍需验证实际 JDK21 CI、Windows 执行、IdP、数据服务、负载、看板、告警路由、持久 Agent 状态、Legacy Tool 结果查询和回滚演练。
