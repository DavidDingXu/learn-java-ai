# Lesson 13 Document Lifecycle Evidence

Status: VERIFIED_LOCAL_DOMAIN

- Implementation commit: `24f2177`
- Aggregate: `KnowledgeDocument`
- Version object: `DocumentVersion`
- Test: `KnowledgeDocumentTest`

## Verified

- 文档标识与内容版本分离，新内容以不可变版本进入同一聚合。
- 发布新版本时当前发布版本退役，聚合始终最多存在一个 `PUBLISHED` 版本。
- 相同内容摘要不能在同一文档中重复创建版本。
- 所有修改携带 expected revision；过期 revision 在聚合变化前被拒绝。
- 生效结束时间必须晚于生效开始时间。

## Evidence Boundary

本报告证明内存中的领域规则和单元测试，不证明数据库乐观锁、跨实例并发或真实文档管理 API 已经验证。数据库结构与持久化冲突将在引入 Flyway/PostgreSQL 后单独报告。
