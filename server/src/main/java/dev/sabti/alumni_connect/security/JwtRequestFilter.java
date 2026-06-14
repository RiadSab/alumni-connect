package dev.sabti.alumni_connect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Reads the Bearer token on each request and, when valid, populates the SecurityContext. It never
// blocks the chain: authorizeHttpRequests decides what an (un)authenticated request may reach.
@Slf4j
@Component
@AllArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final MyUserDetailsService myUserDetailsService;

    // Why a presented token failed, read by RestAuthenticationEntryPoint. Absent when no token was sent.
    public static final String JWT_ERROR_ATTRIBUTE = "jwtErrorCode";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = parseJwt(request);
        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            JwtUtil.TokenStatus status = jwtUtil.checkToken(jwt);
            if (status == JwtUtil.TokenStatus.VALID) {
                authenticate(jwt, request);
            } else {
                // Record why for the entry point, but don't block — authorizeHttpRequests decides.
                request.setAttribute(JWT_ERROR_ATTRIBUTE,
                        status == JwtUtil.TokenStatus.EXPIRED ? "TOKEN_EXPIRED" : "INVALID_TOKEN");
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String jwt, HttpServletRequest request) {
        String email = jwtUtil.getEmailFromToken(jwt);
        // TODO cache userDetails to avoid a DB hit on every request
        UserDetails userDetails = myUserDetailsService.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.info("Authenticated user: {}", email);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
