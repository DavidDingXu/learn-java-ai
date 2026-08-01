# LangChain4j Tool And Structured Output Verification

Status: VERIFIED_READ_ONLY_TOOL_LOOP


## Verified

- A real LangChain4j `@Tool` executes one read-only ticket query in a two-model-call loop.
- The final JSON maps to `TicketDecision` and is validated against the current ticket id.
- Tests assert one tool execution and two model calls, then reject a structured result that names another ticket.

## Verification

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

## External Boundary

No write tool is executed. Production writes must reuse confirmation, action id, idempotency, audit and UNKNOWN reconciliation.
