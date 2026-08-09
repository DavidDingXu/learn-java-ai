package com.xiaoding.javaai.ticket.security;

import com.xiaoding.javaai.ticket.agent.application.ConfirmationActor;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

public final class ConfirmationActorFactory {

    public ConfirmationActor create(Jwt jwt, String requiredActor) {
        if (jwt == null) throw new IllegalArgumentException("jwt must not be null");
        String actor = actorSubject(jwt);
        if (requiredActor == null || requiredActor.isBlank() || !requiredActor.equals(actor)) {
            throw new IllegalArgumentException("delegated token actor must be " + requiredActor);
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new ConfirmationActor(
                jwt.getClaimAsString("tenantId"),
                jwt.getSubject(),
                actor,
                roles == null ? List.of() : roles);
    }

    private static String actorSubject(Jwt jwt) {
        Map<String, Object> actor = jwt.getClaimAsMap("act");
        return actor == null ? null : String.valueOf(actor.get("sub"));
    }
}
