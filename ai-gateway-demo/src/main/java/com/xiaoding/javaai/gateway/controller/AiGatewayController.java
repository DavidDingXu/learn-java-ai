package com.xiaoding.javaai.gateway.controller;

import com.xiaoding.javaai.common.model.AiChatRequest;
import com.xiaoding.javaai.common.model.AiChatResponse;
import com.xiaoding.javaai.gateway.service.AiCallGateway;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiGatewayController {

    private final AiCallGateway aiCallGateway;

    public AiGatewayController(AiCallGateway aiCallGateway) {
        this.aiCallGateway = aiCallGateway;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@RequestBody AiChatRequest request) {
        return aiCallGateway.chat(request);
    }
}
