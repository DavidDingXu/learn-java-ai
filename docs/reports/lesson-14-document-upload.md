# 文档上传验证

Status: IMPLEMENTED_WITH_LOCAL_TESTS

- Use case: `DocumentUploadService`
- HTTP entry: `KnowledgeDocumentController`
- JDBC repository: `JdbcKnowledgeDocumentRepository`
- Local object adapter: `LocalFileDocumentObjectStore`
- Parser: `Utf8TextDocumentContentParser`

## 已验证

- 5 MiB 上限和媒体类型白名单在解析、对象写入和仓储保存前执行。
- 文本解析严格拒绝非法 UTF-8，不使用替换字符掩盖损坏内容。
- 内容使用 SHA-256 摘要，相同文档的重复内容不会产生第二次对象写入。
- 对象键包含 tenant 和 document 前缀；本地文件适配器拒绝逃逸根目录的路径。
- 解析失败时不保存对象，也不创建文档版本。
- 本地文件写入使用同目录临时文件，并在支持时原子替换目标文件。
- 上传接口只从 `KnowledgeAccessScopeProvider` 读取 tenant 和编辑人，不接受请求体或 Header 覆盖；数据库通过 revision 条件更新阻止并发覆盖。
- JDBC 仓储可以从 `knowledge_document` 和 `document_version` 恢复完整聚合，供后续发布和索引读取复用。

## 证据边界

H2 垂直链测试覆盖 JDBC 元数据、对象文件和领域规则，不替代 PostgreSQL 迁移测试。本地文件适配器也没有覆盖 MinIO、S3、OSS 或公司对象存储；接入外部对象存储后还要测试认证、桶策略、服务端加密、恶意文件扫描、保留周期和失败清理。
