# TopK 前权限过滤验证

Status: IMPLEMENTED_WITH_LOCAL_TESTS_AND_EXTERNAL_PROFILE

- Trusted scope: `KnowledgeAccessScope`
- Identity boundary: `KnowledgeAccessScopeProvider`
- Filtered query: `PgVectorSearchQuery`
- Search adapter: `PgVectorKnowledgeChunkSearchRepository`
- External test: `PgVectorExternalIT`

## 本地已验证

- 检索范围只由 `KnowledgeAccessScopeProvider` 提供。本地返回固定范围，生产 JWT 适配器从已验签的 Claim 提取 `tenantId`、`sub` 和 `departmentIds`；两种模式都不接受请求正文覆盖租户或主体。
- SQL 在距离排序和 `LIMIT` 之前约束租户、当前有效的 `PUBLISHED` 业务版本、`document_search_version` 指向的索引版本和 `READ` 权限。
- ACL 是文档级规则；发布替换版本后，新 ACL 立即作用于仍在服务的上一版索引，不会因为索引切换延迟而继续使用旧权限。
- 权限匹配支持当前用户、所属部门和租户级主体；没有部门声明时不会生成空的 `IN` 分支。
- 向量距离和 TopK 只作用于已授权候选，未授权高相似度文档不会先占据候选名额再被 Java 代码删除。
- 权限主体和值均通过 `PreparedStatement` 参数绑定，不拼接用户输入。

## 本地验证

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=FixedKnowledgeAccessScopeProviderTest,JwtKnowledgeAccessScopeFactoryTest,PgVectorSearchQueryTest \
  test
```

该命令检查本地固定范围、生产 JWT Claim 映射，以及权限、版本和时效条件位于 TopK 之前的 SQL 结构；它不启动数据库。

## 外部 pgvector 验证

外部测试会清空并重建目标数据库，只能使用一次性或专用测试库，不属于日常代码回归。`PgVectorExternalIT` 写入一个当前用户可读文档、一个仅 finance 部门可读文档和一个其他租户文档，在 `topK=1` 时应只返回 `allowed-chunk`。Knowledge Service 的数据库连接项位于 `application.yml`，真实密码必须由部署平台的密钥系统覆盖。是否满足该行为以专用测试库上的运行结果为准。

## 证据边界

当前规则覆盖 `USER`、`DEPARTMENT`、`TENANT` 三种允许主体，不覆盖显式拒绝、组织树继承、动态属性策略、权限有效期或 PostgreSQL RLS。固定身份只为本地学习降低门槛；生产身份适配器仍要由公司身份或权限系统提供受信输入。SQL 结构测试也不能替代目标数据库的执行计划和并发测试。
