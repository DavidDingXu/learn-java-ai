# Framework Migration Labs

These modules compare framework behavior behind narrow business contracts. They are not dependencies of the production services.

Run all experiments:

```bash
../mvnw -f pom.xml verify
```

Current test evidence:

- Spring AI Alibaba: Provider options and response mapping, retrieval replacement metrics, confirmation Graph and compatibility decision.
- LangChain4j: AI Services, tenant-scoped RAG, Tool execution, structured output and framework routing.
- AgentScope: Tool permission decisions, human intervention, MCP external tools and A2A task state.

No test in this reactor requires Docker or a real model credential. A real Provider or remote protocol test must be added as a separate, secret-gated integration path and must not replace deterministic unit coverage.
