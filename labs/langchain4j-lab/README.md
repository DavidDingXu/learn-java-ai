# LangChain4j Lab

This lab migrates selected use cases behind business ports instead of replacing the main application architecture.

- `LangChain4jPolicyAnswerAdapter` implements one stable answer port with AI Services.
- `TenantScopedRagAdapter` delegates ACL-aware retrieval to `KnowledgeSearchPort` before content reaches the model.
- `LangChain4jTicketDecisionAdapter` executes a read-only Tool and parses a structured business record.
- `FrameworkCoexistencePolicy` routes by capability, never by leaking framework objects between modules.

```bash
../../mvnw -f ../pom.xml -pl langchain4j-lab test
```
