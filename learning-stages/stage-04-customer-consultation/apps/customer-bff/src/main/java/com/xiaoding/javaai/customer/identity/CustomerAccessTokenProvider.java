package com.xiaoding.javaai.customer.identity;

import org.springframework.security.core.Authentication;

@FunctionalInterface
public interface CustomerAccessTokenProvider {
    CustomerAccessToken current(Authentication authentication);
}
