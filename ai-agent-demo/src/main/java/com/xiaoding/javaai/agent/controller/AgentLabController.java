package com.xiaoding.javaai.agent.controller;

import com.xiaoding.javaai.agent.service.AgentLabService;
import com.xiaoding.javaai.agent.service.react.ReActRunResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/lab")
public class AgentLabController {

    private final AgentLabService agentLabService;

    public AgentLabController(AgentLabService agentLabService) {
        this.agentLabService = agentLabService;
    }

    @PostMapping("/context")
    public AgentLabService.ContextLabResult context(@RequestBody AgentLabService.ContextLabRequest request) {
        return agentLabService.context(request);
    }

    @PostMapping("/react")
    public ReActRunResult react(@RequestBody AgentLabService.ReActLabRequest request) {
        return agentLabService.react(request);
    }
}
