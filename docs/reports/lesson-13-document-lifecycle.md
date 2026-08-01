# 文档生命周期验证

Status: IMPLEMENTED_WITH_LOCAL_TESTS

- Aggregate: `KnowledgeDocument`
- Version object: `DocumentVersion`
- JDBC repository: `JdbcKnowledgeDocumentRepository`
- Schema: Flyway V1-V4
- Test: `KnowledgeDocumentTest`

## 已验证

- 文档标识与内容版本分离，新内容以不可变版本进入同一聚合。
- 发布新版本时当前发布版本退役，聚合始终最多存在一个 `PUBLISHED` 版本。
- 相同内容摘要不能在同一文档中重复创建版本。
- 所有修改携带 expected revision；过期 revision 在聚合变化前被拒绝。
- 生效结束时间必须晚于生效开始时间。
- JDBC 更新使用 revision 条件防止并发覆盖，并能从 `knowledge_document` 和 `document_version` 恢复聚合。
- 发布事务保存版本状态、发布审计、文档级 ACL 和索引任务。业务发布版本与 `document_search_version` 分开管理。

## 证据边界

领域测试和 H2 垂直链测试覆盖版本规则、revision 冲突与 JDBC 持久化。目标 PostgreSQL 上的并发更新、锁等待、迁移时间和备份恢复仍需单独测试；对象文件写入也不在数据库事务内，部署时需要孤儿对象清理或补偿机制。
