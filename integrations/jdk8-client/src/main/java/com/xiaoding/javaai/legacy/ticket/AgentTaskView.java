package com.xiaoding.javaai.legacy.ticket;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentTaskView {

    private String taskId;
    private String caseId;
    private String status;
    private long version;
    private String outcome;
    private ConfirmationView confirmation;
    private String updatedAt;

    public AgentTaskView() {
    }

    public String getTaskId() {
        return taskId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public String getOutcome() {
        return outcome;
    }

    public ConfirmationView getConfirmation() {
        return confirmation;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public static final class ConfirmationView {
        private String confirmationId;
        private String actionId;
        private String toolName;
        private String risk;
        private String requiredRole;
        private Map<String, String> arguments = Collections.emptyMap();
        private long boundTaskVersion;
        private String expiresAt;

        public ConfirmationView() {
        }

        public String getConfirmationId() {
            return confirmationId;
        }

        public String getActionId() {
            return actionId;
        }

        public String getToolName() {
            return toolName;
        }

        public String getRisk() {
            return risk;
        }

        public String getRequiredRole() {
            return requiredRole;
        }

        public Map<String, String> getArguments() {
            return Collections.unmodifiableMap(new LinkedHashMap<String, String>(arguments));
        }

        public long getBoundTaskVersion() {
            return boundTaskVersion;
        }

        public String getExpiresAt() {
            return expiresAt;
        }
    }
}
