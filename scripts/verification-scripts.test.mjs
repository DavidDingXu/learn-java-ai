import assert from "node:assert/strict";
import { execFile, spawnSync } from "node:child_process";
import { readFileSync, statSync } from "node:fs";
import { createServer } from "node:http";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);
const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  return readFileSync(path.join(projectRoot, relativePath), "utf8");
}

test("unit scripts cover all independent build boundaries", () => {
  const shell = read("scripts/verify-unit.sh");
  const powershell = read("scripts/verify-unit.ps1");

  for (const content of [shell, powershell]) {
    assert.match(content, /\*\.test\.mjs/);
    assert.match(content, /pom\.xml/);
    assert.match(content, /labs[\\/]pom\.xml/);
    assert.match(content, /integrations[\\/]jdk8-client[\\/]pom\.xml/);
    assert.match(content, /JAVA_AI_MAIN_JAVA_HOME/);
    assert.match(content, /JAVA_AI_JDK8_HOME/);
    assert.match(content, /apps[\\/]customer-web/);
    assert.match(content, /npm(?:\.cmd)?/);
    assert.match(content, /(?:^|["'\s])ci(?:["'\s]|$)/m);
    assert.match(content, /typecheck/);
    assert.match(content, /test/);
    assert.match(content, /build/);
    assert.doesNotMatch(content, /column[\\/]scripts|JAVA_AI_REQUIRE_COLUMN_TESTS/);
  }

  assert.match(shell, /npm --prefix "\$CUSTOMER_WEB_DIR" ci/);
  assert.match(shell, /npm --prefix "\$CUSTOMER_WEB_DIR" run typecheck/);
  assert.match(shell, /npm --prefix "\$CUSTOMER_WEB_DIR" test/);
  assert.match(shell, /npm --prefix "\$CUSTOMER_WEB_DIR" run build/);
  assert.match(powershell, /@\("--prefix", \$CustomerWeb, "ci"/);
  assert.match(powershell, /@\("--prefix", \$CustomerWeb, "run", "typecheck"\)/);
  assert.match(powershell, /@\("--prefix", \$CustomerWeb, "test"\)/);
  assert.match(powershell, /@\("--prefix", \$CustomerWeb, "run", "build"\)/);
});

test("GitHub Verify reaches Customer Web through the shared build entrypoint", () => {
  const workflow = read(".github/workflows/verify.yml");
  const buildShell = read("scripts/verify-build.sh");
  const buildPowershell = read("scripts/verify-build.ps1");

  assert.match(workflow, /actions\/setup-node@v6/);
  assert.match(workflow, /node-version:\s*24/);
  assert.match(workflow, /scripts\/verify-build\.sh/);
  assert.match(buildShell, /verify-unit\.sh/);
  assert.match(buildPowershell, /verify-unit\.ps1/);
});

test("Spring Boot modules are HTTP applications packaged as executable jars", () => {
  for (const relativePom of ["services/knowledge-service/pom.xml", "apps/customer-bff/pom.xml"]) {
    const pom = read(relativePom);
    assert.match(pom, /<artifactId>spring-boot-starter-webflux<\/artifactId>/);
    assert.doesNotMatch(pom, /<artifactId>spring-boot-starter-web<\/artifactId>/);
    assert.doesNotMatch(pom, /<artifactId>spring-boot-starter<\/artifactId>/);
    assert.match(pom, /<artifactId>spring-boot-maven-plugin<\/artifactId>/);
    assert.match(pom, /<goal>repackage<\/goal>/);
  }

  const ticketPom = read("services/ticket-agent-service/pom.xml");
  assert.match(ticketPom, /<artifactId>spring-boot-starter-web<\/artifactId>/);
  assert.match(ticketPom, /<artifactId>spring-boot-maven-plugin<\/artifactId>/);
  assert.match(ticketPom, /<goal>repackage<\/goal>/);
});

test("Java 8 selection never relies on macOS java_home fallback", () => {
  for (const relativePath of [
    "scripts/verify-unit.sh",
    "scripts/verify-unit.ps1",
    "scripts/verify-integration.sh",
    "scripts/verify-integration.ps1",
  ]) {
    assert.doesNotMatch(read(relativePath), /java_home\s+-v\s+1\.8/);
  }
});

test("shell verification scripts are executable", { skip: process.platform === "win32" }, () => {
  for (const relativePath of [
    "scripts/verify-unit.sh",
    "scripts/verify-integration.sh",
    "scripts/run-contract-eval.sh",
    "scripts/run-live-model-eval.sh",
    "scripts/run-retrieval-eval.sh",
    "scripts/run-security-regression.sh",
    "scripts/release-gate.sh",
  ]) {
    assert.notEqual(statSync(path.join(projectRoot, relativePath)).mode & 0o111, 0);
  }
});

test("security regression scripts combine authorization tests and external Agent cases", () => {
  const shell = read("scripts/run-security-regression.sh");
  const powershell = read("scripts/run-security-regression.ps1");

  for (const content of [shell, powershell]) {
    assert.match(content, /KnowledgeJwtSecurityTest/);
    assert.match(content, /TicketAgentJwtSecurityTest/);
    assert.match(content, /BusinessToolCatalogTest/);
    assert.match(content, /datasets[\\/]security[\\/]agent-security-v1\.jsonl/);
    assert.match(content, /security-eval/);
    assert.match(content, /JAVA_AI_AGENT_CREATE_TOKEN/);
    assert.match(content, /JAVA_AI_AGENT_RUN_TOKEN/);
    assert.match(content, /JAVA_AI_AGENT_READ_TOKEN/);
  }
});

test("unit and release gates preserve fast-regression and external-evidence boundaries", () => {
  const unitShell = read("scripts/verify-unit.sh");
  const unitPowershell = read("scripts/verify-unit.ps1");
  const releaseShell = read("scripts/release-gate.sh");
  const releasePowershell = read("scripts/release-gate.ps1");

  for (const content of [unitShell, unitPowershell]) {
    assert.match(content, /node/);
    assert.doesNotMatch(content, /docker\s+(?:run|compose)/i);
  }
  for (const content of [releaseShell, releasePowershell]) {
    assert.match(content, /verify-unit/);
    assert.match(content, /JAVA_AI_RELEASE_REQUIRE_EXTERNAL/);
    assert.match(content, /verify-integration/);
    assert.match(content, /sk-/);
  }
});

test("retrieval evaluation scripts target an authenticated external environment", () => {
  const shell = read("scripts/run-retrieval-eval.sh");
  const powershell = read("scripts/run-retrieval-eval.ps1");

  for (const content of [shell, powershell]) {
    assert.match(content, /JAVA_AI_RETRIEVAL_BASE_URL/);
    assert.match(content, /JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN/);
    assert.match(content, /datasets[\\/]retrieval[\\/]golden-set-v1\.jsonl/);
    assert.match(content, /retrieval-eval/);
    assert.match(content, /min-recall/);
    assert.match(content, /max-p95-ms/);
  }
});

test("shell integration verification refuses a missing external environment", {
  skip: process.platform === "win32",
}, () => {
  const env = { ...process.env };
  delete env.JAVA_AI_EXTERNAL_BASE_URL;

  const result = spawnSync("bash", [path.join(projectRoot, "scripts/verify-integration.sh")], {
    cwd: projectRoot,
    env,
    encoding: "utf8",
  });

  assert.equal(result.status, 2);
  assert.match(result.stderr, /JAVA_AI_EXTERNAL_BASE_URL is required/);
});

test("shell integration verification accepts only an UP health response", {
  skip: process.platform === "win32",
}, async (t) => {
  const server = createServer((request, response) => {
    if (request.url === "/actuator/health") {
      response.writeHead(200, { "content-type": "application/json" });
      response.end(JSON.stringify({ status: "UP" }));
      return;
    }
    response.writeHead(404).end();
  });

  try {
    await new Promise((resolve, reject) => {
      server.once("error", reject);
      server.listen(0, "127.0.0.1", resolve);
    });
  } catch (error) {
    if (error?.code === "EPERM") {
      t.skip("sandbox does not permit binding a loopback test server");
      return;
    }
    throw error;
  }
  t.after(() => new Promise((resolve) => server.close(resolve)));

  const address = server.address();
  assert.notEqual(address, null);
  assert.equal(typeof address, "object");

  const { stdout } = await execFileAsync(
    "bash",
    [path.join(projectRoot, "scripts/verify-integration.sh")],
    {
      cwd: projectRoot,
      env: {
        ...process.env,
        JAVA_AI_EXTERNAL_BASE_URL: `http://127.0.0.1:${address.port}`,
      },
    },
  );

  assert.match(stdout, /External health smoke passed/);
  assert.match(stdout, /one HTTP health endpoint only/);
});

test("PowerShell integration verification has the same missing-environment guard", () => {
  const powershell = read("scripts/verify-integration.ps1");
  assert.match(powershell, /JAVA_AI_EXTERNAL_BASE_URL is required/);
  assert.match(powershell, /exit 2/);
  assert.match(powershell, /status -ne "UP"/);
  assert.match(powershell, /one HTTP health endpoint only/);
});

test("PowerShell evaluation and smoke scripts run Maven with the selected main JDK", () => {
  const runtime = read("scripts/main-java-runtime.ps1");
  assert.match(runtime, /JAVA_AI_MAIN_JAVA_HOME/);
  assert.match(runtime, /\$env:JAVA_HOME\s*=\s*\$JavaHome/);
  assert.match(runtime, /\$env:Path\s*=.*Join-Path \$JavaHome 'bin'/);
  assert.match(runtime, /bin\\java\.exe/);
  assert.match(runtime, /bin\\javac\.exe/);
  assert.match(runtime, /\$Major -lt 21/);

  for (const relativePath of [
    "scripts/run-contract-eval.ps1",
    "scripts/run-live-model-eval.ps1",
    "scripts/run-live-model-smoke.ps1",
    "scripts/run-retrieval-eval.ps1",
    "scripts/run-agent-eval.ps1",
    "scripts/run-agent-live-model-smoke.ps1",
    "scripts/run-security-regression.ps1",
  ]) {
    const content = read(relativePath);
    assert.match(content, /main-java-runtime\.ps1/, relativePath);
    assert.match(content, /Enter-JavaAiMainJdk/, relativePath);
    assert.match(content, /Restore-JavaAiEnvironment/, relativePath);
    assert.doesNotMatch(content, /(?:^|\s)java\s+-jar/, relativePath);
  }
});
