package dev.sabti.alumni_connect.rate_limiting;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RateLimitPolicyResolver {
    private static final Set<String> UPLOAD_PATHS = Set.of(
            "/api/candidates/me/resume", "/api/candidates/me/photo", "/api/companies/me/logo");
    private final Map<String, RateLimitPolicy> policies;
    private final boolean enabled;

    public RateLimitPolicyResolver(RateLimitProperties properties) {
        properties.validate();
        enabled = properties.isEnabled();
        policies = properties.getPolicies().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> entry.getValue().toPolicy(entry.getKey())));
    }

    Optional<RateLimitPolicy> sourcePolicy(HttpServletRequest request) {
        String path = path(request);
        if (!enabled || !isApi(path)) return Optional.empty();
        if ("POST".equals(request.getMethod())) {
            String id = switch (path) {
                case "/api/auth/login" -> "source-login";
                case "/api/auth/forgot-password" -> "source-forgot-password";
                case "/api/auth/reset-password" -> "source-reset-password";
                case "/api/auth/refresh" -> "source-refresh";
                case "/api/auth/logout" -> "source-logout";
                default -> null;
            };
            if (id != null) return policy(id);
            if (path.startsWith("/api/auth/register/")) return policy("source-registration");
        }
        if (under(path, "/api/alumni/claim") || under(path, "/api/employment/confirm")
                || under(path, "/api/mentoring/confirm")) return policy("source-confirmation");
        if ("GET".equals(request.getMethod()) && (under(path, "/api/companies")
                || under(path, "/api/job-offers"))) return policy("source-public-read");
        return policy("source-default");
    }

    Optional<RateLimitPolicy> authenticatedPolicy(HttpServletRequest request) {
        String path = path(request);
        if (!enabled || !isApi(path)) return Optional.empty();
        if ("POST".equals(request.getMethod()) && path.equals("/api/auth/change-password")) {
            return policy("user-change-password");
        }
        if (under(path, "/api/auth")) return Optional.empty();
        if ("POST".equals(request.getMethod()) && UPLOAD_PATHS.contains(path)) return policy("user-upload");
        return policy("user-default");
    }

    private Optional<RateLimitPolicy> policy(String id) {
        return Optional.of(policies.get(id));
    }

    private static boolean isApi(String path) {
        return under(path, "/api");
    }

    private static boolean under(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
