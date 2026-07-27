# Lesson 17 ACL Before TopK Evidence

Status: IMPLEMENTED_WITH_LOCAL_TESTS_AND_EXTERNAL_PROFILE

- Trusted scope: `KnowledgeAccessScope`
- JWT mapping: `JwtKnowledgeAccessScopeFactory`
- Filtered query: `PgVectorSearchQuery`
- Search adapter: `PgVectorKnowledgeChunkSearchRepository`
- External test: `PgVectorExternalIT`

## Verified Locally

- 检索范围只从已经通过资源服务器校验的 JWT 中提取 `tenantId`、`sub` 和 `departmentIds`，不接受请求正文覆盖租户或主体。
- SQL 在距离排序和 `LIMIT` 之前约束租户、当前有效的 `PUBLISHED` 业务版本、`document_search_version` 指向的索引版本和 `READ` 权限。
- ACL 是文档级规则；发布替换版本后，新 ACL 立即作用于仍在服务的上一版索引，不会因为索引切换延迟而继续使用旧权限。
- 权限匹配支持当前用户、所属部门和租户级主体；没有部门声明时不会生成空的 `IN` 分支。
- 向量距离和 TopK 只作用于已授权候选，未授权高相似度文档不会先占据候选名额再被 Java 代码删除。
- 权限主体和值均通过 `PreparedStatement` 参数绑定，不拼接用户输入。

## Local Verification

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=JwtKnowledgeAccessScopeFactoryTest,PgVectorSearchQueryTest \
  test
```

该命令检查 JWT Claim 到检索范围的映射，以及权限、版本和时效条件位于 TopK 之前的 SQL 结构；它不启动数据库。

## External pgvector Verification

外部测试会清空并重建目标数据库，只能使用一次性或专用测试库，不属于默认演示步骤。`PgVectorExternalIT` 写入一个当前用户可读文档、一个仅 finance 部门可读文档和一个其他租户文档，在 `topK=1` 时应只返回 `allowed-chunk`。Knowledge Service 的生产连接项位于 `application.yml` 的 `production` 段，真实密码必须由部署平台的密钥系统覆盖。是否满足该行为以专用测试库上的运行结果为准。

## Evidence Boundary

当前规则覆盖 `USER`、`DEPARTMENT`、`TENANT` 三种允许主体，不覆盖显式拒绝、组织树继承、动态属性策略、权限有效期或 PostgreSQL RLS。`JwtKnowledgeAccessScopeFactory` 的输入前提是 JWT 已由 Spring Security 校验；它自身不校验签名、issuer、audience 和过期时间。SQL 结构测试也不能替代目标数据库的执行计划和并发测试。
