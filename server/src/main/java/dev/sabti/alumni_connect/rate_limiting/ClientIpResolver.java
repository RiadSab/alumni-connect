package dev.sabti.alumni_connect.rate_limiting;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpResolver {
    public String resolve(HttpServletRequest request) {
        // Production Tomcat resolves Caddy's sanitized X-Forwarded-For first.
        // Never read caller-supplied forwarding headers directly here.
        return request.getRemoteAddr();
    }
}
