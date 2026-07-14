package com.xiaoding.javaai.knowledge.answer.web;

import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestionCommand;
import com.xiaoding.javaai.knowledge.answer.application.AnswerStreamEvent;
import com.xiaoding.javaai.knowledge.answer.application.StreamKnowledgeAnswer;
import com.xiaoding.javaai.knowledge.security.JwtKnowledgeAccessScopeFactory;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/knowledge/answers")
public final class KnowledgeAnswerController {

    private final AnswerKnowledgeQuestion useCase;
    private final StreamKnowledgeAnswer streamUseCase;
    private final Clock clock;
    private final JwtKnowledgeAccessScopeFactory accessScopeFactory = new JwtKnowledgeAccessScopeFactory();

    public KnowledgeAnswerController(
            AnswerKnowledgeQuestion useCase,
            StreamKnowledgeAnswer streamUseCase,
            Clock clock
    ) {
        this.useCase = useCase;
        this.streamUseCase = streamUseCase;
        this.clock = clock;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<KnowledgeAnswerResponse> answer(
            @Valid @RequestBody KnowledgeAnswerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return useCase.answer(command(request, jwt))
                .map(KnowledgeAnswerResponse::from);
    }

    @PostMapping(
            path = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    Flux<ServerSentEvent<AnswerStreamEvent>> stream(
            @Valid @RequestBody KnowledgeAnswerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return streamUseCase.stream(command(request, jwt))
                .map(event -> ServerSentEvent.builder(event)
                        .event(eventName(event))
                        .build());
    }

    private AnswerKnowledgeQuestionCommand command(KnowledgeAnswerRequest request, Jwt jwt) {
        return new AnswerKnowledgeQuestionCommand(
                request.question(),
                request.conversationContext().toApplication(),
                accessScopeFactory.create(jwt),
                Instant.now(clock)
        );
    }

    private static String eventName(AnswerStreamEvent event) {
        return switch (event) {
            case AnswerStreamEvent.MetadataEvent ignored -> "metadata";
            case AnswerStreamEvent.DeltaEvent ignored -> "delta";
            case AnswerStreamEvent.HeartbeatEvent ignored -> "heartbeat";
            case AnswerStreamEvent.CitationEvent ignored -> "citation";
            case AnswerStreamEvent.CompletedEvent ignored -> "completed";
            case AnswerStreamEvent.ErrorEvent ignored -> "error";
        };
    }
}
