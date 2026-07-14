package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentityFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent/tasks")
public final class AgentTaskController {

    private final AgentTaskIntakeService service;
    private final DelegatedTicketIdentityFactory identityFactory;

    public AgentTaskController(
            AgentTaskIntakeService service,
            DelegatedTicketIdentityFactory identityFactory
    ) {
        this.service = service;
        this.identityFactory = identityFactory;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AgentTaskReceipt> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(min = 8, max = 128) String idempotencyKey,
            @Valid @RequestBody AgentTaskWebRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(service.accept(
                        identityFactory.create(jwt, "customer-bff"),
                        idempotencyKey,
                        request.toApplication()));
    }
}
