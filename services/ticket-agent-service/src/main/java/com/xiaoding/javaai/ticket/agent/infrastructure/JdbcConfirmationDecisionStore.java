package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.ConfirmationDecisionReceipt;
import com.xiaoding.javaai.ticket.agent.application.ConfirmationDecisionStore;
import com.xiaoding.javaai.ticket.agent.application.ConfirmationIdempotencyConflictException;
import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class JdbcConfirmationDecisionStore implements ConfirmationDecisionStore {

    private static final int LOCK_BUCKETS = 64;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final Duration leaseDuration;

    public JdbcConfirmationDecisionStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this(jdbc, transactions, Clock.systemUTC(), Duration.ofSeconds(30));
    }

    public JdbcConfirmationDecisionStore(
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            Clock clock,
            Duration leaseDuration
    ) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.transactions = java.util.Objects.requireNonNull(transactions, "transactions must not be null");
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
        this.leaseDuration = java.util.Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
    }

    @Override
    public StoredDecision executeOnce(
            String trustedPrincipalScope,
            String idempotencyKey,
            String fingerprint,
            Supplier<ConfirmationDecisionReceipt> action
    ) {
        Claim claim = claim(trustedPrincipalScope, idempotencyKey, fingerprint);
        if (claim.receipt() != null) return new StoredDecision(claim.receipt(), true);

        try {
            ConfirmationDecisionReceipt receipt = action.get();
            complete(trustedPrincipalScope, idempotencyKey, claim.owner(), receipt);
            return new StoredDecision(receipt, false);
        } catch (RuntimeException error) {
            release(trustedPrincipalScope, idempotencyKey, claim.owner());
            throw error;
        }
    }

    private Claim claim(String scope, String key, String fingerprint) {
        return transactions.execute(status -> {
            int bucket = Math.floorMod((scope + '\n' + key).hashCode(), LOCK_BUCKETS);
            jdbc.queryForObject(
                    "select bucket_id from agent_confirmation_lock_bucket where bucket_id = ? for update",
                    Integer.class, bucket);
            Instant now = clock.instant();
            List<DecisionRow> rows = find(scope, key);
            if (!rows.isEmpty()) {
                DecisionRow existing = rows.getFirst();
                if (!existing.fingerprint().equals(fingerprint)) {
                    throw new ConfirmationIdempotencyConflictException(key);
                }
                if (existing.receipt() != null) return new Claim(null, existing.receipt());
                // A stale write claim must be reconciled with the downstream action before takeover.
                throw new ConfirmationDecisionInProgressException(key);
            }

            String owner = UUID.randomUUID().toString();
            Instant leaseUntil = now.plus(leaseDuration);
            jdbc.update("""
                            insert into agent_confirmation_decision (
                                principal_scope, idempotency_key, request_fingerprint, decision_status,
                                lease_owner, lease_until, created_at, updated_at
                            ) values (?, ?, ?, 'PENDING', ?, ?, ?, ?)
                            """,
                    scope, key, fingerprint, owner, Timestamp.from(leaseUntil),
                    Timestamp.from(now), Timestamp.from(now));
            return new Claim(owner, null);
        });
    }

    private void complete(String scope, String key, String owner, ConfirmationDecisionReceipt receipt) {
        transactions.executeWithoutResult(status -> {
            int updated = jdbc.update("""
                            update agent_confirmation_decision
                               set decision_status = 'COMPLETED', lease_owner = null, lease_until = null,
                                   task_id = ?, task_state = ?, action_id = ?, tool_status = ?, audit_id = ?,
                                   task_version = ?, updated_at = ?
                             where principal_scope = ? and idempotency_key = ?
                               and decision_status = 'PENDING' and lease_owner = ?
                            """,
                    receipt.taskId(), receipt.state().name(), receipt.actionId(), receipt.toolStatus(),
                    receipt.auditId(), receipt.taskVersion(), Timestamp.from(clock.instant()),
                    scope, key, owner);
            if (updated != 1) throw new IllegalStateException("confirmation decision lease was lost");
        });
    }

    private void release(String scope, String key, String owner) {
        transactions.executeWithoutResult(status -> jdbc.update("""
                        delete from agent_confirmation_decision
                         where principal_scope = ? and idempotency_key = ?
                           and decision_status = 'PENDING' and lease_owner = ?
                        """,
                scope, key, owner));
    }

    private List<DecisionRow> find(String scope, String key) {
        return jdbc.query("""
                        select request_fingerprint, decision_status,
                               task_id, task_state, action_id, tool_status, audit_id, task_version
                          from agent_confirmation_decision
                         where principal_scope = ? and idempotency_key = ?
                        """,
                (resultSet, rowNumber) -> {
                    ConfirmationDecisionReceipt receipt = null;
                    if ("COMPLETED".equals(resultSet.getString("decision_status"))) {
                        receipt = new ConfirmationDecisionReceipt(
                                resultSet.getString("task_id"),
                                AgentTaskState.valueOf(resultSet.getString("task_state")),
                                resultSet.getString("action_id"),
                                resultSet.getString("tool_status"),
                                resultSet.getString("audit_id"),
                                resultSet.getLong("task_version"), false);
                    }
                    return new DecisionRow(resultSet.getString("request_fingerprint"), receipt);
                },
                scope, key);
    }

    private record Claim(String owner, ConfirmationDecisionReceipt receipt) {
    }

    private record DecisionRow(
            String fingerprint,
            ConfirmationDecisionReceipt receipt
    ) {
    }
}
