# Knowledge Service

企业知识文档、检索与 RAG 回答服务。该模块拥有文档业务版本、ACL、索引任务、分块、检索版本指针和引用映射，不拥有客户会话或工单业务状态。

## 主要边界

- `/api/v1/knowledge/documents`：文档上传、版本发布与 ACL。
- `/api/v1/knowledge/answers`：完整回答和 SSE 流式回答。
- `/internal/v1/knowledge/index-tasks`：受 `knowledge:index` 权限保护的索引任务入口。
- `/internal/v1/knowledge/retrieval/evaluations`：受 `knowledge:eval` 权限保护的检索评测入口。

公开契约以 [`contracts/openapi/knowledge-service-v1.yaml`](../../contracts/openapi/knowledge-service-v1.yaml) 为准。租户、主体和部门范围来自已验证 JWT，不从业务请求体读取。

## 运行与测试

```bash
./mvnw -pl services/knowledge-service test
./mvnw -pl services/knowledge-service spring-boot:run
```

默认 `demo` Profile 仅启动应用并提供 health，不会伪造数据库、检索或模型结果。真实模型连接见 [Live Model Smoke](../../docs/runbooks/live-model-smoke.md)，完整写入链路见 [Knowledge Ingestion](../../docs/runbooks/knowledge-ingestion.md)。

生产部署还需接入 PostgreSQL/pgvector、对象存储、公司 IdP 与真实 Provider，并验证迁移、容量、备份恢复和检索质量。
