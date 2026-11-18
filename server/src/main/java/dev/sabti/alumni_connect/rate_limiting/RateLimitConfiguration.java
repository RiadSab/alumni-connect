package dev.sabti.alumni_connect.rate_limiting;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {
    @Bean
    RateLimitService rateLimitService(RateLimitProperties properties) {
        return new RateLimitService(properties);
    }

    @Bean
    RateLimitPolicyResolver rateLimitPolicyResolver(RateLimitProperties properties) {
        return new RateLimitPolicyResolver(properties);
    }

    @Bean
    ClientIpResolver clientIpResolver() {
        return new ClientIpResolver();
    }

    @Bean
    RateLimitResponseWriter rateLimitResponseWriter(ObjectMapper mapper) {
        return new RateLimitResponseWriter(mapper);
    }

    @Bean
    SourceRateLimitFilter sourceRateLimitFilter(RateLimitPolicyResolver policies, ClientIpResolver ips,
                                              RateLimitService service, RateLimitResponseWriter writer) {
        return new SourceRateLimitFilter(policies, ips, service, writer);
    }

    @Bean
    AuthenticatedRateLimitFilter authenticatedRateLimitFilter(RateLimitPolicyResolver policies,
                                                            RateLimitService service, RateLimitResponseWriter writer) {
        return new AuthenticatedRateLimitFilter(policies, service, writer);
    }

    // SecurityConfig controls ordering; do not also register container filters.
    @Bean
    FilterRegistrationBean<SourceRateLimitFilter> sourceRateLimitRegistration(SourceRateLimitFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<AuthenticatedRateLimitFilter> authenticatedRateLimitRegistration(AuthenticatedRateLimitFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
