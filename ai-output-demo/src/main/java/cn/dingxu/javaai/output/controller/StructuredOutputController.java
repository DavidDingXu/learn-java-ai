package cn.dingxu.javaai.output.controller;

import cn.dingxu.javaai.output.TicketAdviceResponse;
import cn.dingxu.javaai.output.service.AiJsonParser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/output")
public class StructuredOutputController {

    private final AiJsonParser aiJsonParser;

    public StructuredOutputController(AiJsonParser aiJsonParser) {
        this.aiJsonParser = aiJsonParser;
    }

    @PostMapping("/ticket-advice/parse")
    public TicketAdviceResponse parse(@RequestBody String rawJson) {
        return aiJsonParser.parseStrict(rawJson, TicketAdviceResponse.class);
    }
}
