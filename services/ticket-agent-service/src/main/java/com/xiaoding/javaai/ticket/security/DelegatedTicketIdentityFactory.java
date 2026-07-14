package com.xiaoding.javaai.ticket.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

public final class DelegatedTicketIdentityFactory {

    public DelegatedTicketIdentity create(Jwt jwt, String requiredActor) {
        if (jwt == null) throw new IllegalArgumentException("jwt must not be null");
        String actor = actorSubject(jwt);
        if (requiredActor == null || requiredActor.isBlank() || !requiredActor.equals(actor)) {
            throw new IllegalArgumentException("delegated token actor must be " + requiredActor);
        }
        return new DelegatedTicketIdentity(
                jwt.getClaimAsString("tenantId"),
                jwt.getSubject(),
                actor,
                stringList(jwt.getClaimAsStringList("roles")),
                stringList(jwt.getClaimAsStringList("departmentIds"))
        );
    }

    private static String actorSubject(Jwt jwt) {
        Map<String, Object> actor = jwt.getClaimAsMap("act");
        return actor == null ? null : String.valueOf(actor.get("sub"));
    }

    private static List<String> stringList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
