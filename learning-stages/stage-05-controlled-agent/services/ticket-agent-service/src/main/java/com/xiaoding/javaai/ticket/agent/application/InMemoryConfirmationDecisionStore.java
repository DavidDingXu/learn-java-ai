package com.xiaoding.javaai.ticket.agent.application;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class InMemoryConfirmationDecisionStore implements ConfirmationDecisionStore {

    private final ConcurrentMap<String, StoredEntry> entries = new ConcurrentHashMap<>();

    @Override
    public StoredDecision executeOnce(
            String trustedPrincipalScope,
            String idempotencyKey,
            String fingerprint,
            Supplier<ConfirmationDecisionReceipt> action
    ) {
        String scopedKey = trustedPrincipalScope + "\n" + idempotencyKey;
        AtomicReference<StoredDecision> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        entries.compute(scopedKey, (ignored, existing) -> {
            if (existing == null) {
                ConfirmationDecisionReceipt receipt = action.get();
                result.set(new StoredDecision(receipt, false));
                return new StoredEntry(fingerprint, receipt);
            }
            if (!existing.fingerprint().equals(fingerprint)) {
                failure.set(new ConfirmationIdempotencyConflictException(idempotencyKey));
                return existing;
            }
            result.set(new StoredDecision(existing.receipt(), true));
            return existing;
        });
        if (failure.get() != null) throw failure.get();
        return result.get();
    }

    private record StoredEntry(String fingerprint, ConfirmationDecisionReceipt receipt) {
    }
}
