package com.xiaoding.javaai.customer.consultation.infrastructure;

public final class DownstreamServiceException extends RuntimeException {

    private final int statusCode;

    public DownstreamServiceException(String service, int statusCode) {
        super(service + " returned HTTP " + statusCode);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
