package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.TicketIdentityProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent/tasks")
public final class AgentTaskController {

    private final AgentTaskIntakeService service;
    private final TicketIdentityProvider identityProvider;

    public AgentTaskController(
            AgentTaskIntakeService service,
            TicketIdentityProvider identityProvider
    ) {
        this.service = service;
        this.identityProvider = identityProvider;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AgentTaskReceipt> create(
            Authentication authentication,
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(min = 8, max = 128) String idempotencyKey,
            @Valid @RequestBody AgentTaskWebRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(service.accept(
                        identityProvider.current(authentication, "customer-bff"),
                        idempotencyKey,
                        request.toApplication()));
    }
}
