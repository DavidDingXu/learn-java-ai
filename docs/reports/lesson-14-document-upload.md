# Lesson 14 Document Upload Evidence

Status: VERIFIED_LOCAL_APPLICATION

- Implementation commit: `24f2177`
- Use case: `DocumentUploadService`
- Local object adapter: `LocalFileDocumentObjectStore`
- Parser: `Utf8TextDocumentContentParser`

## Verified

- 5 MiB 上限和媒体类型白名单在解析、对象写入和仓储保存前执行。
- 文本解析严格拒绝非法 UTF-8，不使用替换字符掩盖损坏内容。
- 内容使用 SHA-256 摘要，相同文档的重复内容不会产生第二次对象写入。
- 对象键包含 tenant 和 document 前缀；本地文件适配器拒绝逃逸根目录的路径。
- 解析失败时不保存对象，也不创建文档版本。
- 本地文件写入使用同目录临时文件，并在支持时原子替换目标文件。

## Evidence Boundary

这是本地文件适配器的代码回归证据，不代表 MinIO、S3、OSS 或公司对象存储已联调。外部对象存储还需要认证、桶策略、服务端加密、恶意文件扫描、保留周期和失败清理验证。
