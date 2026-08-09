package com.xiaoding.javaai.ticket.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixedTicketIdentityProviderTest {

    @Test
    void keeps_the_local_subject_and_uses_the_actor_required_by_each_endpoint() {
        FixedTicketIdentityProvider provider = new FixedTicketIdentityProvider(
                "tenant-a", "local-user", List.of("TICKET_OPERATOR"), List.of("support"));

        DelegatedTicketIdentity identity = provider.current(null, "ticket-agent-worker");

        assertThat(identity.tenantId()).isEqualTo("tenant-a");
        assertThat(identity.subjectId()).isEqualTo("local-user");
        assertThat(identity.actorId()).isEqualTo("ticket-agent-worker");
        assertThat(identity.roles()).containsExactly("TICKET_OPERATOR");
    }
}
