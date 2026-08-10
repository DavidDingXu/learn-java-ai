package com.xiaoding.javaai.eval.contract;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ContractValidator {

    private static final List<String> OPEN_API_FILES = List.of(
            "openapi/knowledge-service-v1.yaml"
    );
    private static final List<SchemaFixture> SCHEMA_FIXTURES = List.of();

    public ContractValidationReport validateRepository(Path contractsRoot) {
        List<String> errors = new ArrayList<>();
        int openApiCount = validateOpenApi(contractsRoot, errors);
        int schemaCount = 0;
        int positiveCount = 0;
        int negativeCount = 0;

        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
        for (SchemaFixture fixture : SCHEMA_FIXTURES) {
            try {
                String schemaText = read(contractsRoot, fixture.schema());
                Schema schema = registry.getSchema(schemaText, InputFormat.JSON);
                schemaCount++;

                List<com.networknt.schema.Error> positiveErrors = schema.validate(
                        read(contractsRoot, fixture.positive()), InputFormat.JSON);
                if (positiveErrors.isEmpty()) {
                    positiveCount++;
                } else {
                    errors.add(fixture.positive() + " should be valid: " + positiveErrors);
                }

                List<com.networknt.schema.Error> negativeErrors = schema.validate(
                        read(contractsRoot, fixture.negative()), InputFormat.JSON);
                if (!negativeErrors.isEmpty()) {
                    negativeCount++;
                } else {
                    errors.add(fixture.negative() + " should be rejected");
                }
            } catch (RuntimeException | IOException exception) {
                errors.add(fixture.schema() + ": " + exception.getMessage());
            }
        }
        return new ContractValidationReport(
                openApiCount, schemaCount, positiveCount, negativeCount, errors);
    }

    private static int validateOpenApi(Path contractsRoot, List<String> errors) {
        int count = 0;
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        for (String relativePath : OPEN_API_FILES) {
            Path path = contractsRoot.resolve(relativePath).normalize();
            SwaggerParseResult result = new OpenAPIV3Parser()
                    .readLocation(path.toUri().toString(), null, options);
            if (result.getOpenAPI() == null || !result.getMessages().isEmpty()) {
                errors.add(relativePath + ": " + result.getMessages());
            } else {
                count++;
            }
        }
        return count;
    }

    private static String read(Path root, String relativePath) throws IOException {
        return Files.readString(root.resolve(relativePath).normalize());
    }

    private record SchemaFixture(String schema, String positive, String negative) {
    }
}
