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

@Slf4j
@Component
@AllArgsConstructor
//extends OncePerRequestFilter, Spring security base class ensuring the filter runs once per request
// the token is received here before reaching the controller
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final MyUserDetailsService myUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, // representing an HTTP request
                                    HttpServletResponse response, // representing an HTTP response
                                    FilterChain filterChain) // provides a way to invoke the next filter in the chain
                            // when we call filterChain.doFilter(request, response) we are passing control to the next filter in the chain
        throws ServletException, IOException{

        // Skip JWT processing for public endpoints
        String requestPath = request.getRequestURI();
        if ((requestPath.startsWith("/api/auth/") && !requestPath.equals("/api/auth/change-password")) ||
            requestPath.equals("/api/companies/all-names") ||
            requestPath.startsWith("/api/job-offers/public")) {
            log.debug("Skipping JWT filter for public endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = parseJwt(request);
        if(jwt != null && jwtUtil.validateToken(jwt)
                && SecurityContextHolder.getContext().getAuthentication() == null )
        {
            String email = jwtUtil.getEmailFromToken(jwt);
            // load user details associated with the email extracted from the token, who logged in
            // UserDetails is a core interface in Spring Security that represents a user in the system
            // TODO implement cache for userDetails to avoid hitting DB on each request
            // TODO implement UserDetailsService in a way that it fetches user from DB
            UserDetails userDetails = myUserDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, // the principal, the who, controllers access via @AuthenticationPrincipal UserDetails user
                            // Spring security uses this for authorization decisions
                            null, // no credentials (password) needed here, jwt validate user, keep token stateless: no sensitive data stored in server
                            userDetails.getAuthorities() // get the roles/permissions of the user
                    );

            // WebAuthenticationDetailsSource: used to build details about the current web authentication request
            // It captures remote address and session ID http session (if any)
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken); // set the authentication in the security context
            // after this, the user is authenticated for the current request, and we can access user details in controllers via @AuthenticationPrincipal
            log.info("Authenticated user: {}, setting security context", email);

            filterChain.doFilter(request, response); // continue the filter chain
        }
        else {
            log.debug("No valid JWT token found");
            throw new RuntimeException("No valid JWT token found");
        }
    }

    private String parseJwt(HttpServletRequest request){
        String headerAuth = request.getHeader("Authorization");
        //StringUtils.hasText("S") checks if the string is not null, not empty, and contains at least one non-whitespace character
        if(StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")){
            return headerAuth.substring(7);
            // "Bearer " is 7 characters long, we remove it to get the actual token
        }
        return null;
    }
}
