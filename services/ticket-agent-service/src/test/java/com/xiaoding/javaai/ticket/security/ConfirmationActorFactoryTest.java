package com.xiaoding.javaai.ticket.security;

import com.xiaoding.javaai.ticket.agent.application.ConfirmationActor;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmationActorFactoryTest {

    private final ConfirmationActorFactory factory = new ConfirmationActorFactory();

    @Test
    void maps_employee_identity_and_roles_from_the_verified_token() {
        ConfirmationActor actor = factory.create(jwt("jdk8-crm"), "jdk8-crm");

        assertThat(actor.tenantId()).isEqualTo("tenant-a");
        assertThat(actor.subjectId()).isEqualTo("employee-7");
        assertThat(actor.actorId()).isEqualTo("jdk8-crm");
        assertThat(actor.roles()).containsExactly("TICKET_OPERATOR");
    }

    @Test
    void rejects_a_confirmation_submitted_by_an_untrusted_client_actor() {
        assertThatThrownBy(() -> factory.create(jwt("browser"), "jdk8-crm"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor");
    }

    private static Jwt jwt(String actor) {
        return new Jwt(
                "token-value",
                Instant.parse("2026-07-13T08:00:00Z"),
                Instant.parse("2026-07-13T08:05:00Z"),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "employee-7",
                        "tenantId", "tenant-a",
                        "act", Map.of("sub", actor),
                        "roles", List.of("TICKET_OPERATOR")
                ));
    }
}
