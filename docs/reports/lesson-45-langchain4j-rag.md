# LangChain4j RAG 接口边界验证

Status: VERIFIED_CONTRACT_REUSE


## 已验证

- 隔离实验定义最小化 `KnowledgeSearchPort`，语义与主检索链路一致，但不导入 Knowledge Service 内部实现。
- LangChain4j 适配器向该接口传递原始问题、请求级 `KnowledgeAccessScope` 和固定 TopK。
- 检索结果继续使用项目自有的 `KnowledgeSnippet` 与 `PolicyAnswer` DTO，不向外暴露框架检索对象。
- 聚焦测试验证问题、权限范围和 TopK 的传递，以及候选来源 ID 到模型请求和业务回答的映射。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

## 外部验证边界

`PolicyAnswer` 中的 source ID 是检索候选，不是从模型输出中解析出的引用。该实验不验证生产身份，不执行 ACL SQL，也没有检查跨租户候选和模型引用。它同样不复制主服务的 pgvector 索引；这些行为要由主服务集成测试、Golden Set 和目标基础设施验证。
