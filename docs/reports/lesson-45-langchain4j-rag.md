# Lesson 45 LangChain4j RAG Contract Evidence

Status: VERIFIED_CONTRACT_REUSE

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- The LangChain4j adapter uses the existing tenant-scoped `KnowledgeSearchPort`.
- Authorized evidence and citations remain business DTOs outside framework memory and retrieval objects.
- Tests reject cross-tenant or ungrounded results through the stable contract.

## Verification

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

## External Boundary

The lab does not duplicate the production pgvector index. Real retrieval comparison still uses the main Golden Set and target infrastructure.
