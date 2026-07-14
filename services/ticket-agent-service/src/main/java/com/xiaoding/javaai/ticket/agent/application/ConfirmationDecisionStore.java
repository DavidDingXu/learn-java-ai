package com.xiaoding.javaai.ticket.agent.application;

import java.util.function.Supplier;

public interface ConfirmationDecisionStore {

    StoredDecision executeOnce(
            String trustedPrincipalScope,
            String idempotencyKey,
            String fingerprint,
            Supplier<ConfirmationDecisionReceipt> action
    );

    record StoredDecision(ConfirmationDecisionReceipt receipt, boolean duplicate) {
    }
}
