package com.xiaoding.javaai.output.controller;

import com.xiaoding.javaai.output.TicketAdviceResponse;
import com.xiaoding.javaai.output.service.AiJsonParser;
import com.xiaoding.javaai.output.service.TicketAdviceGenerationService;
import com.xiaoding.javaai.output.service.TicketAdviceGenerationService.TicketAdviceGenerationInput;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/output")
public class StructuredOutputController {

    private final AiJsonParser aiJsonParser;
    private final TicketAdviceGenerationService generationService;

    public StructuredOutputController(AiJsonParser aiJsonParser,
                                      TicketAdviceGenerationService generationService) {
        this.aiJsonParser = aiJsonParser;
        this.generationService = generationService;
    }

    @PostMapping("/ticket-advice/parse")
    public TicketAdviceResponse parse(@RequestBody String rawJson) {
        return aiJsonParser.parseStrict(rawJson, TicketAdviceResponse.class);
    }

    @PostMapping("/ticket-advice/generate")
    public TicketAdviceGenerationResponse generate(@RequestBody TicketAdviceGenerationRequest request) {
        var output = generationService.generate(new TicketAdviceGenerationInput(request.ticket(), request.policy()));
        return new TicketAdviceGenerationResponse(output.mode(), output.prompt(), output.rawOutput(), output.advice());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StructuredOutputError handleBadStructuredOutput(IllegalArgumentException error) {
        return new StructuredOutputError("STRUCTURED_OUTPUT_INVALID", "AI output cannot become a valid business object", error.getMessage());
    }

    public record TicketAdviceGenerationRequest(String ticket, String policy) {
    }

    public record TicketAdviceGenerationResponse(String mode, String prompt, String rawOutput, TicketAdviceResponse advice) {
    }

    public record StructuredOutputError(String code, String message, String detail) {
    }
}
