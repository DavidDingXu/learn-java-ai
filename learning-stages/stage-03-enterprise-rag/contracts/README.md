# 阶段 03 接口契约

本阶段只拥有扩展后的 `openapi/knowledge-service-v1.yaml`：在阶段 02 的问答与 SSE 上增加文档上传、发布、索引任务和检索评测。Customer BFF、Agent 与 Legacy Tool 属于后续阶段，不在这里占位。

所有文档和检索操作都从服务端身份取得 tenant 与 ACL 范围，请求体不能声明自己的访问身份。接口与 `rag-learning-journey.http` 一一对应。
