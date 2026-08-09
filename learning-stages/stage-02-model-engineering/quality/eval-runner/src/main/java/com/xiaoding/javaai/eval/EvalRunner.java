package com.xiaoding.javaai.eval;

import com.xiaoding.javaai.eval.contract.ContractValidationReport;
import com.xiaoding.javaai.eval.contract.ContractValidator;
import com.xiaoding.javaai.eval.model.ContractFixtureServer;
import com.xiaoding.javaai.eval.model.EvalDataset;
import com.xiaoding.javaai.eval.model.EvalDatasetLoader;
import com.xiaoding.javaai.eval.model.EvalMode;
import com.xiaoding.javaai.eval.model.EvalReport;
import com.xiaoding.javaai.eval.model.EvalReportWriter;
import com.xiaoding.javaai.eval.model.KnowledgeAnswerHttpClient;
import com.xiaoding.javaai.eval.model.ModelInteractionEvaluator;

import java.net.URI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EvalRunner {

    private EvalRunner() {
    }

    public static String version() {
        return "0.1.0";
    }

    public static void main(String[] args) {
        if (args.length == 2 && "contract-validate".equals(args[0])) {
            ContractValidationReport report = new ContractValidator()
                    .validateRepository(Path.of(args[1]).toAbsolutePath().normalize());
            System.out.printf(
                    "openapi=%d schemas=%d positive=%d negative=%d valid=%s%n",
                    report.validatedOpenApi(),
                    report.validatedSchemas(),
                    report.positiveFixtures(),
                    report.negativeFixtures(),
                    report.valid());
            report.errors().forEach(error -> System.err.println("ERROR " + error));
            if (!report.valid()) {
                throw new IllegalStateException("Contract validation failed");
            }
            return;
        }
        if (args.length > 0 && "contract-eval".equals(args[0])) {
            Map<String, String> options = parseOptions(args, 1);
            try (ContractFixtureServer server = ContractFixtureServer.start()) {
                runEval(options, server.baseUrl(), EvalMode.CONTRACT_FIXTURE);
            }
            return;
        }
        if (args.length > 0 && "model-eval".equals(args[0])) {
            Map<String, String> options = parseOptions(args, 1);
            URI baseUrl = URI.create(required(options, "base-url"));
            EvalMode mode = EvalMode.valueOf(required(options, "mode"));
            runEval(options, baseUrl, mode);
            return;
        }
        System.out.println("eval-runner " + version());
        System.out.println("usage: contract-validate <contracts-directory>");
        System.out.println("       contract-eval --dataset <jsonl> --prompt-version <id> --environment-id <id> --report <path-prefix>");
        System.out.println("       model-eval --dataset <jsonl> --base-url <url> --mode <LIVE_MODEL|CONTRACT_FIXTURE> [--bearer-token-file <path>] --prompt-version <id> --environment-id <id> --report <path-prefix>");
    }

    private static void runEval(Map<String, String> options, URI baseUrl, EvalMode mode) {
        EvalDataset dataset = new EvalDatasetLoader().load(Path.of(required(options, "dataset")));
        String bearerToken = optionalCredential(options, "bearer-token");
        EvalReport report = new ModelInteractionEvaluator(new KnowledgeAnswerHttpClient(bearerToken))
                .evaluate(
                        dataset,
                        baseUrl,
                        mode,
                        codeRevision(options),
                        required(options, "prompt-version"),
                        required(options, "environment-id")
                );
        Path reportPrefix = Path.of(required(options, "report"));
        new EvalReportWriter().write(
                report,
                Path.of(reportPrefix + ".json"),
                Path.of(reportPrefix + ".md")
        );
        System.out.printf(
                "dataset=%s mode=%s model=%s passed=%d failed=%d tokens=%d%n",
                report.datasetVersion(), report.mode(), report.model(),
                report.passed(), report.failed(), report.totalTokens()
        );
        if (report.failed() > 0) {
            throw new IllegalStateException("Model interaction evaluation failed");
        }
    }

    private static Map<String, String> parseOptions(String[] args, int start) {
        if ((args.length - start) % 2 != 0) {
            throw new IllegalArgumentException("options must use --name value pairs");
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = start; index < args.length; index += 2) {
            String name = args[index];
            if (!name.startsWith("--")) throw new IllegalArgumentException("invalid option: " + name);
            options.put(name.substring(2), args[index + 1]);
        }
        return options;
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required option --" + name);
        }
        return value;
    }

    private static String codeRevision(Map<String, String> options) {
        return options.getOrDefault("commit", "local-reader-run");
    }

    static String requiredCredential(Map<String, String> options, String name) {
        String credential = optionalCredential(options, name);
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("missing required option --" + name + "-file");
        }
        return credential;
    }

    private static String optionalCredential(Map<String, String> options, String name) {
        String inline = options.get(name);
        String file = options.get(name + "-file");
        if (inline != null && !inline.isBlank() && file != null && !file.isBlank()) {
            throw new IllegalArgumentException(
                    "use either --" + name + " or --" + name + "-file, not both");
        }
        if (file != null && !file.isBlank()) {
            try {
                String credential = Files.readString(Path.of(file)).trim();
                if (credential.isBlank()) {
                    throw new IllegalArgumentException("credential file is blank: " + file);
                }
                return credential;
            } catch (IOException error) {
                throw new IllegalArgumentException("cannot read credential file: " + file, error);
            }
        }
        return inline == null ? null : inline.trim();
    }

    private static int requiredInt(Map<String, String> options, String name) {
        try {
            return Integer.parseInt(required(options, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("option --" + name + " must be an integer", exception);
        }
    }

    private static long requiredLong(Map<String, String> options, String name) {
        try {
            return Long.parseLong(required(options, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("option --" + name + " must be an integer", exception);
        }
    }

    private static double requiredDouble(Map<String, String> options, String name) {
        try {
            return Double.parseDouble(required(options, name));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("option --" + name + " must be a number", exception);
        }
    }
}
