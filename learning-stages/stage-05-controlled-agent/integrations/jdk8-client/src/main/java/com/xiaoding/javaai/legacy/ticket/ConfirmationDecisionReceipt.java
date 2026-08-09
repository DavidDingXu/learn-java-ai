package com.xiaoding.javaai.legacy.ticket;

public final class ConfirmationDecisionReceipt {

    private String taskId;
    private String state;
    private String actionId;
    private String toolStatus;
    private String auditId;
    private long taskVersion;
    private boolean duplicate;

    public ConfirmationDecisionReceipt() {
    }

    public String getTaskId() {
        return taskId;
    }

    public String getState() {
        return state;
    }

    public String getActionId() {
        return actionId;
    }

    public String getToolStatus() {
        return toolStatus;
    }

    public String getAuditId() {
        return auditId;
    }

    public long getTaskVersion() {
        return taskVersion;
    }

    public boolean isDuplicate() {
        return duplicate;
    }
}
