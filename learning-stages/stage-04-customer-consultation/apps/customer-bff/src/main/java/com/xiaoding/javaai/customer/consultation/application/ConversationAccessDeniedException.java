package com.xiaoding.javaai.customer.consultation.application;

public final class ConversationAccessDeniedException extends RuntimeException {
    public ConversationAccessDeniedException() {
        super("conversation does not belong to the authenticated customer");
    }
}
