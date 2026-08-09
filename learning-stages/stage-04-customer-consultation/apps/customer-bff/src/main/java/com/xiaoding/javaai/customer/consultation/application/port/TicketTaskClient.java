package com.xiaoding.javaai.customer.consultation.application.port;

import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffReceipt;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffSnapshot;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface TicketTaskClient {
    Mono<TicketHandoffReceipt> createHandoff(
            DelegatedAccessToken token,
            String idempotencyKey,
            TicketHandoffSnapshot snapshot
    );
}
