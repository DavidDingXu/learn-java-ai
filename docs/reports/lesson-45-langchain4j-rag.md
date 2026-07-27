# Lesson 45 LangChain4j RAG Contract Evidence

Status: VERIFIED_CONTRACT_REUSE

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- The isolated lab defines a minimal `KnowledgeSearchPort` aligned with the main retrieval semantics without importing Knowledge Service internals.
- The LangChain4j adapter passes the original query, a request-scoped `KnowledgeAccessScope` and fixed TopK to that port.
- Search results remain project-owned `KnowledgeSnippet` and `PolicyAnswer` DTOs outside framework retrieval objects.
- The focused test verifies query/scope/TopK propagation and candidate source ID mapping into the model request and business answer.

## Verification

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

## External Boundary

The `PolicyAnswer` source IDs are retrieved candidates, not parsed model citations. The lab does not authenticate a JWT, execute ACL SQL, reject a cross-tenant candidate or validate model-produced citations. It also does not duplicate the production pgvector index. Those behaviors require the main service integration tests, Golden Set and target infrastructure.
