package com.xiaoding.javaai.customer.identity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixedCustomerAccessTokenProviderTest {

    @Test
    void returns_the_configured_local_identity_without_authentication() {
        FixedCustomerAccessTokenProvider provider = new FixedCustomerAccessTokenProvider(
                "tenant-a", "local-user", List.of("customer"), List.of("support"));

        CustomerAccessToken token = provider.current(null);

        assertThat(token.tokenValue()).isEqualTo("local-customer-access");
        assertThat(token.identity()).isEqualTo(new CustomerIdentity(
                "local-user", "tenant-a", List.of("customer"), List.of("support")));
    }
}
