# Lesson 15 Document Chunking Evidence

Status: IMPLEMENTED_WITH_DETERMINISTIC_TESTS

- Chunker: `PolicyDocumentChunker`
- Indexer: `DocumentVersionIndexingService`
- Dataset fixture: `datasets/knowledge/refund-policy-chunking-v1.md`
- Test: `PolicyDocumentChunkerTest`

## Verified

- Markdown 标题层级进入每个 chunk 的 heading path。
- `第十条` 一类条款编号进入独立 clause 元数据。
- tenant、document、version、chunk policy version 和 ordinal 共同组成 chunk 标识。
- CRLF 与 LF 规范化后产生相同 chunk ID。
- 超长内容按句子边界拆分；无法按句子拆分时才执行硬切分，结果不超过策略上限。
- chunk ID 由规范化内容和完整版本元数据计算，可用于幂等增量替换。

## Evidence Boundary

当前索引 Worker 已使用该切分器，但字符上限不等同于目标模型的 Token 上限。公司语料接入后需要结合 tokenizer、召回坏案例和引用粒度重新确定参数；修改策略时应升级 `chunkPolicyVersion` 并重建索引。
