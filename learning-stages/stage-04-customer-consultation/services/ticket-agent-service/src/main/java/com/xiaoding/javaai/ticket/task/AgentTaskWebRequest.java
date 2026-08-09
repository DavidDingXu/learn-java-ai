package com.xiaoding.javaai.ticket.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record AgentTaskWebRequest(
        @NotBlank @Size(max = 64) String caseId,
        @NotBlank @Size(max = 1000) String objective,
        @NotNull @Size(max = 32) Map<@NotBlank @Size(max = 128) String,
                @NotBlank @Size(max = 2000) String> businessContext
) {
    AgentTaskRequest toApplication() {
        return new AgentTaskRequest(caseId, objective, businessContext);
    }
}
