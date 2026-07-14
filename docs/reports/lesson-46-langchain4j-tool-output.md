# Lesson 46 LangChain4j Tool And Structured Output Evidence

Status: VERIFIED_READ_ONLY_TOOL_LOOP

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- A real LangChain4j `@Tool` executes one read-only ticket query in a two-model-call loop.
- The final JSON maps to `TicketDecision` and is validated against the current ticket id.
- Tests assert one tool execution, two model calls and structured result validation.

## Verification

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

## External Boundary

No write tool is executed. Production writes must reuse confirmation, action id, idempotency, audit and UNKNOWN reconciliation.
