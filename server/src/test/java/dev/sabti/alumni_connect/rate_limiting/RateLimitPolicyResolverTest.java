package dev.sabti.alumni_connect.rate_limiting;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPolicyResolverTest {
    private final RateLimitPolicyResolver policies = new RateLimitPolicyResolver(RateLimitServiceTest.properties());

    @Test
    void contextPathAndQueryValuesDoNotChangeTheBudget() {
        var request = new MockHttpServletRequest("GET", "/portal/api/job-offers");
        request.setContextPath("/portal");
        request.setQueryString("q=developer");
        assertThat(policies.sourcePolicy(request).orElseThrow().id()).isEqualTo("source-public-read");
        request.setQueryString("q=another-value");
        assertThat(policies.sourcePolicy(request).orElseThrow().id()).isEqualTo("source-public-read");
    }

    @Test
    void authEndpointsHaveSeparateSourcePoliciesAndNoSubmittedAccountBuckets() {
        var refresh = new MockHttpServletRequest("POST", "/api/auth/refresh");
        assertThat(policies.sourcePolicy(refresh).orElseThrow().id()).isEqualTo("source-refresh");
        assertThat(policies.authenticatedPolicy(refresh)).isEmpty();
        var change = new MockHttpServletRequest("POST", "/api/auth/change-password");
        assertThat(policies.authenticatedPolicy(change).orElseThrow().id()).isEqualTo("user-change-password");
    }

    @Test
    void onlyApiRoutesAreLimitedAndDisableFlagSkipsPolicySelection() {
        assertThat(policies.sourcePolicy(new MockHttpServletRequest("GET", "/assets/site.css"))).isEmpty();
        assertThat(policies.sourcePolicy(new MockHttpServletRequest("GET", "/apiary"))).isEmpty();
        var properties = RateLimitServiceTest.properties();
        properties.setEnabled(false);
        var disabled = new RateLimitPolicyResolver(properties);
        var request = new MockHttpServletRequest("GET", "/api/candidates/me");
        assertThat(disabled.sourcePolicy(request)).isEmpty();
        assertThat(disabled.authenticatedPolicy(request)).isEmpty();
    }
}
