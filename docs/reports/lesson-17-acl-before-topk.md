# Lesson 17 ACL Before TopK Evidence

Status: VERIFIED_LOCAL_ACCESS_QUERY_CONTRACT_WITH_EXTERNAL_PROFILE

- Trusted scope: `KnowledgeAccessScope`
- JWT mapping: `JwtKnowledgeAccessScopeFactory`
- Filtered query: `PgVectorSearchQuery`
- Search adapter: `PgVectorKnowledgeChunkSearchRepository`
- External test: `PgVectorExternalIT`

## Verified Locally

- 检索范围只从已经通过资源服务器校验的 JWT 中提取 `tenantId`、`sub` 和 `departmentIds`，不接受请求正文覆盖租户或主体。
- SQL 在距离排序和 `LIMIT` 之前约束租户、`PUBLISHED` 版本、生效时间和 `READ` 权限。
- 权限匹配支持当前用户、所属部门和租户级主体；没有部门声明时不会生成空的 `IN` 分支。
- 向量距离和 TopK 只作用于已授权候选，未授权高相似度文档不会先占据候选名额再被 Java 代码删除。
- 权限主体和值均通过 `PreparedStatement` 参数绑定，不拼接用户输入。

## Local Verification

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=JwtKnowledgeAccessScopeFactoryTest,PgVectorSearchQueryTest \
  test
```

预期 4 个测试全部通过。该命令验证 JWT Claim 到检索范围的映射，以及权限、版本和时效条件位于 TopK 之前的 SQL 合同；它不启动数据库。

## External pgvector Verification

外部测试会清空并重建目标数据库，只能使用一次性或专用测试库：

```bash
export JAVA_AI_POSTGRES_URL='jdbc:postgresql://127.0.0.1:5432/java_ai_test'
export JAVA_AI_POSTGRES_USER='java_ai'
export JAVA_AI_POSTGRES_PASSWORD='replace-with-test-password'

./mvnw -pl services/knowledge-service verify -Pexternal-integration
```

`PgVectorExternalIT` 写入一个当前用户可读文档、一个仅 finance 部门可读文档和一个其他租户文档，在 `topK=1` 时预期只返回 `allowed-chunk`。只有该集成测试真实通过，才能把结论扩大到 PostgreSQL/pgvector 的查询行为。

## Evidence Boundary

本报告覆盖当前 `USER`、`DEPARTMENT`、`TENANT` 三种允许规则，不覆盖显式拒绝、组织树继承、动态属性策略、权限有效期或 PostgreSQL RLS。`JwtKnowledgeAccessScopeFactory` 的输入前提是 JWT 已由 Spring Security 验证；它自身不验证签名、issuer、audience 和过期时间。SQL 合同测试也不能替代真实数据库的执行计划与并发验证。
