import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath);
  assert.equal(existsSync(absolutePath), true, `${relativePath} must exist`);
  return readFileSync(absolutePath, "utf8");
}

test("public request contracts never accept caller-supplied identity", () => {
  const contracts = [
    read("contracts/openapi/knowledge-service-v1.yaml"),
    read("contracts/openapi/customer-bff-v1.yaml"),
    read("contracts/openapi/agent-task-v1.yaml"),
    read("contracts/openapi/legacy-tool-v1.yaml"),
    read("contracts/json-schema/agent-task-request-v1.schema.json"),
    read("contracts/json-schema/tool-action-command-v1.schema.json"),
  ].join("\n");

  for (const forbidden of ["userId", "tenantId", "roles", "departments"]) {
    assert.doesNotMatch(contracts, new RegExp(`^[ \\t\"']*${forbidden}[\"']?\\s*:`, "m"));
  }
  assert.match(contracts, /bearerAuth/);
  assert.match(contracts, /Idempotency-Key/);
});

test("security dataset covers every delegated identity boundary", () => {
  const dataset = read("datasets/security/jwt-boundary-cases-v1.jsonl");

  for (const caseId of [
    "valid-delegated-token",
    "invalid-signature",
    "wrong-issuer",
    "wrong-audience",
    "missing-scope",
    "missing-tenant",
    "missing-actor",
    "expired-token",
    "customer-token-direct",
    "spoofed-identity-headers",
  ]) {
    assert.match(dataset, new RegExp(`"caseId":"${caseId}"`));
  }
});

test("agent and legacy OpenAPI files describe the complete workflow and response receipts", () => {
  const agentApi = read("contracts/openapi/agent-task-v1.yaml");
  const legacyApi = read("contracts/openapi/legacy-tool-v1.yaml");
  const toolSchema = read("contracts/json-schema/tool-action-command-v1.schema.json");
  const legacyDto = read(
    "integrations/jdk8-client/src/main/java/com/xiaoding/javaai/legacy/contract/ToolActionCommand.java",
  );

  for (const path of [
    "/api/v1/agent/tasks/{taskId}:",
    "/api/v1/agent/tasks/{taskId}/runs:",
    "/api/v1/agent/tasks/{taskId}/confirmation:",
    "/api/v1/agent/tasks/{taskId}/audit:",
  ]) {
    assert.match(agentApi, new RegExp(path.replace(/[{}]/g, "\\$&")));
  }
  assert.match(agentApi, /ConfirmationDecisionReceipt/);
  assert.match(agentApi, /AgentAuditEvent/);
  assert.match(legacyApi, /ToolActionReceipt/);
  assert.match(toolSchema, /ISSUE_REFUND/);
  assert.doesNotMatch(legacyDto, /idempotencyKey/);
});

test("customer BFF contract exposes stable downstream failures on non-streaming operations", () => {
  const api = read("contracts/openapi/customer-bff-v1.yaml");

  for (const operationId of [
    "answerCustomerConsultation",
    "retryCustomerAnswer",
    "handoffCustomerConsultation",
  ]) {
    const start = api.indexOf(`operationId: ${operationId}`);
    assert.notEqual(start, -1, `${operationId} must exist`);
    const nextOperation = api.indexOf("operationId:", start + 12);
    const block = api.slice(start, nextOperation === -1 ? api.length : nextOperation);
    assert.match(block, /'502':[\s\S]*DownstreamServiceFailed/);
    assert.match(block, /'504':[\s\S]*DownstreamTimeout/);
  }

  assert.match(api, /KNOWLEDGE_STREAM_TIMEOUT/);
  assert.match(api, /code: DOWNSTREAM_SERVICE_FAILED/);
  assert.match(api, /code: DOWNSTREAM_TIMEOUT/);
});

test("lesson 05 and 06 reports bind security and contract evidence", () => {
  for (const reportPath of [
    "docs/reports/lesson-05-delegated-identity.md",
    "docs/reports/lesson-06-contract-boundaries.md",
    "docs/reports/milestone-06.md",
  ]) {
    const report = read(reportPath);
    assert.match(report, /HTTP\/OpenAPI|JWT|JDK8/);
    assert.doesNotMatch(report, /生产身份平台已验证|production identity platform verified/i);
    assert.doesNotMatch(report, /Tag Rule|必须指向|must point/i);
  }
});

test("live evaluation scripts use the local identity without enabling database adapters", () => {
  const shell = read("scripts/run-live-model-eval.sh");
  const powershell = read("scripts/run-live-model-eval.ps1");

  for (const script of [shell, powershell]) {
    assert.doesNotMatch(script, /java-ai\.security\.jwt\./);
    assert.doesNotMatch(script, /spring\.profiles\.active/);
    assert.match(script, /java-ai\.knowledge\.mode=classpath/);
    assert.match(script, /java-ai\.security\.mode=fixed/);
    assert.match(script, /spring\.ai\.model\.embedding=none/);
    assert.match(script, /spring\.flyway\.enabled=false/);
    assert.match(script, /DataSourceAutoConfiguration/);
    assert.match(script, /FlywayAutoConfiguration/);
  }
});
