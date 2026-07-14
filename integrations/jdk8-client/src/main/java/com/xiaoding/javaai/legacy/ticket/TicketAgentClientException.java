package com.xiaoding.javaai.legacy.ticket;

public class TicketAgentClientException extends RuntimeException {

    private final int statusCode;
    private final String code;
    private final boolean retryable;

    public TicketAgentClientException(
            int statusCode,
            String code,
            String message,
            boolean retryable
    ) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
