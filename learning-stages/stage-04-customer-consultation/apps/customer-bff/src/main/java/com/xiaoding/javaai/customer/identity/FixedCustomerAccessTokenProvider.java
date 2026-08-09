package com.xiaoding.javaai.customer.identity;

import org.springframework.security.core.Authentication;

import java.util.List;

public final class FixedCustomerAccessTokenProvider implements CustomerAccessTokenProvider {

    private static final String LOCAL_TOKEN = "local-customer-access";

    private final CustomerAccessToken accessToken;

    public FixedCustomerAccessTokenProvider(
            String tenantId,
            String subject,
            List<String> roles,
            List<String> departments
    ) {
        this.accessToken = new CustomerAccessToken(
                LOCAL_TOKEN,
                new CustomerIdentity(subject, tenantId, roles, departments));
    }

    @Override
    public CustomerAccessToken current(Authentication authentication) {
        return accessToken;
    }
}
