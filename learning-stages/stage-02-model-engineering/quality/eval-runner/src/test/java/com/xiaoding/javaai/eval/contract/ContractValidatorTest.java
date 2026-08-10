package com.xiaoding.javaai.eval.contract;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractValidatorTest {

    private final ContractValidator validator = new ContractValidator();
    private final Path contracts = Path.of("../../contracts").toAbsolutePath().normalize();

    @Test
    void validatesAllOpenApiAndJsonSchemaDocuments() {
        ContractValidationReport report = validator.validateRepository(contracts);

        assertTrue(report.valid(), () -> String.join(System.lineSeparator(), report.errors()));
        assertEquals(1, report.validatedOpenApi());
        assertEquals(0, report.validatedSchemas());
        assertEquals(0, report.positiveFixtures());
        assertEquals(0, report.negativeFixtures());
        assertTrue(report.errors().isEmpty());
    }
}
