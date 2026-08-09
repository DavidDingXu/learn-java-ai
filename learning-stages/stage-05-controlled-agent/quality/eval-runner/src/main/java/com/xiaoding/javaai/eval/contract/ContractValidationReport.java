package com.xiaoding.javaai.eval.contract;

import java.util.List;

public record ContractValidationReport(
        int validatedOpenApi,
        int validatedSchemas,
        int positiveFixtures,
        int negativeFixtures,
        List<String> errors
) {
    public ContractValidationReport {
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
