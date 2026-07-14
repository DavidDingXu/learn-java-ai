import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath);
  assert.equal(existsSync(absolutePath), true, `${relativePath} must exist`);
  return readFileSync(absolutePath, "utf8");
}

function filesUnder(relativeDirectory) {
  const absoluteDirectory = path.join(projectRoot, relativeDirectory);
  return readdirSync(absoluteDirectory).flatMap((entry) => {
    const relativePath = path.join(relativeDirectory, entry);
    return statSync(path.join(projectRoot, relativePath)).isDirectory()
      ? filesUnder(relativePath)
      : [relativePath];
  });
}

test("Spring AI remains inside the knowledge infrastructure adapter", () => {
  const javaFiles = filesUnder("services/knowledge-service/src/main/java")
    .filter((file) => file.endsWith(".java"));

  for (const javaFile of javaFiles) {
    const source = read(javaFile);
    if (source.includes("org.springframework.ai")) {
      assert.match(javaFile, /(?:answer|retrieval)\/infrastructure\//);
    }
  }

  const adapter = read("services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/answer/infrastructure/SpringAiKnowledgeAnswerModel.java");
  assert.match(adapter, /Mono\.fromCallable/);
  assert.match(adapter, /Schedulers\.boundedElastic\(\)/);
});

test("lesson 04 ships deterministic policy without exposing verification modes", () => {
  const policy = read("services/knowledge-service/src/main/resources/knowledge/refund-policy-v1.md");
  const metadata = read("services/knowledge-service/src/main/resources/knowledge/refund-policy-v1.properties");
  const runtime = read("services/knowledge-service/src/main/resources/application.yml");
  const openApi = read("contracts/openapi/knowledge-service-v1.yaml");
  const response = read("services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/answer/web/KnowledgeAnswerResponse.java");

  assert.match(policy, /1 到 5 个工作日/);
  assert.match(metadata, /documentId=refund-policy/);
  assert.match(metadata, /version=v1/);
  assert.match(metadata, /sectionId=arrival-time/);
  assert.match(runtime, /\$\{JAVA_AI_CHAT_API_KEY}/);
  assert.doesNotMatch(runtime, /execution-mode/);
  assert.doesNotMatch(openApi, /executionMode|LOCAL_DISABLED|PROVIDER_PROTOCOL_FIXTURE/);
  assert.doesNotMatch(response, /ExecutionMode|executionMode/);
});

test("live model smoke is cross-platform, secret-gated and report-producing", () => {
  const shell = read("scripts/run-live-model-smoke.sh");
  const powershell = read("scripts/run-live-model-smoke.ps1");
  const workflow = read(".github/workflows/live-model-smoke.yml");
  const runbook = read("docs/runbooks/live-model-smoke.md");
  const report = read("docs/reports/lesson-04-live-model-smoke.md");

  for (const content of [shell, powershell, workflow, runbook]) {
    assert.match(content, /JAVA_AI_CHAT_API_KEY/);
    assert.match(content, /JAVA_AI_CHAT_BASE_URL/);
    assert.match(content, /JAVA_AI_CHAT_MODEL/);
  }

  assert.match(shell, /LiveModelSmokeIT/);
  assert.match(powershell, /LiveModelSmokeIT/);
  assert.match(workflow, /workflow_dispatch/);
  assert.match(workflow, /actions\/upload-artifact/);
  assert.match(report, /Status: (?:NOT_RUN|LIVE_MODEL)/);
  if (/Status: LIVE_MODEL/.test(report)) {
    assert.match(report, /Execution mode: `LIVE_MODEL`/);
    assert.match(report, /Trace ID: `(?:untraced|[0-9a-f]{32})`/);
    assert.doesNotMatch(report, /sk-[A-Za-z0-9]{12,}/);
  } else {
    assert.match(report, /No model credentials were available|未提供模型凭据/i);
  }
  assert.doesNotMatch(report, /PROVIDER_PROTOCOL_FIXTURE.*LIVE_MODEL|LIVE_MODEL.*PROVIDER_PROTOCOL_FIXTURE/s);
});

test("shell live smoke refuses missing model credentials", {
  skip: process.platform === "win32",
}, () => {
  const scriptPath = path.join(projectRoot, "scripts/run-live-model-smoke.sh");
  const env = { ...process.env };
  delete env.JAVA_AI_CHAT_API_KEY;
  delete env.JAVA_AI_CHAT_BASE_URL;
  delete env.JAVA_AI_CHAT_MODEL;

  const result = spawnSync(scriptPath, [], {
    cwd: projectRoot,
    env,
    encoding: "utf8",
  });

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /JAVA_AI_CHAT_API_KEY is required/);
});

test("lesson 04 milestone status follows the real model evidence", () => {
  const milestone = read("docs/reports/milestone-04.md");
  const report = read("docs/reports/lesson-04-live-model-smoke.md");

  if (/VERIFIED_LIVE_MODEL/.test(milestone)) {
    assert.match(report, /Status: LIVE_MODEL/);
    assert.match(milestone, /VERIFIED_LIVE_MODEL/);
    assert.match(milestone, /milestone-04-real-model/);
  } else {
    assert.match(milestone, /PENDING_LIVE_MODEL/);
    assert.match(milestone, /provider protocol fixture/i);
    assert.match(milestone, /must not create|不得创建/i);
  }
});
