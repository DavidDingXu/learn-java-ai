# 阶段 02 接口契约

本阶段只公开 `openapi/knowledge-service-v1.yaml`，对应普通回答与 SSE 回答。文档上传、检索评测、Customer BFF、Agent 和 Legacy Tool 尚未进入系统，因此不会提前出现在本目录。

身份事实由服务端固定身份或已验证 Token 提供，请求体只包含问题和受限会话上下文。读者启动 `KnowledgeServiceApplication` 后，可直接用阶段 HTTP 文件观察 JSON 与 SSE 是否符合这份接口。
