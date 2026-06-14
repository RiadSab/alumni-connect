package dev.sabti.alumni_connect.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {
    private final JwtRequestFilter jwtRequestFilter;
    private final MyUserDetailsService myUserDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

   
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Return ApiError JSON for filter-chain auth failures instead of an empty body.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                    // An error forward carries no JWT; keep /error public so it isn't re-masked as 401/403.
                    .requestMatchers("/error").permitAll()
                    // Before the /api/auth/** permitAll, so it isn't made public (login/register are).
                    .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()
                    .requestMatchers("/api/auth/**").permitAll()
                    // Public company browse feeds the "join an existing company" registration flow.
                    .requestMatchers(HttpMethod.GET, "/api/companies", "/api/companies/**").permitAll()
                    .requestMatchers(HttpMethod.PATCH, "/api/companies/me").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/companies/me/logo").authenticated()
                    // Private, company-scoped views — before the broad job-offers GET so the wildcard
                    // doesn't expose them publicly.
                    .requestMatchers(HttpMethod.GET, "/api/job-offers/*/applications").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/job-offers/me").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/job-offers/recommended").hasRole("CANDIDATE")
                    // Candidate self-profile — before the admin-only /api/candidates/* by-id matcher.
                    .requestMatchers(HttpMethod.GET, "/api/candidates/me").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/candidates/me").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/candidates/me/resume").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/candidates/me/resume").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/candidates/me/photo").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/candidates/me/photo").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/candidates/*").hasRole("ADMINISTRATOR")
                    // Company-user self-profile — before the admin-only /api/company-users/* matcher.
                    .requestMatchers(HttpMethod.GET, "/api/company-users").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/company-users/me").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/company-users/me").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/company-users/*/role").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/company-users/*").hasRole("ADMINISTRATOR")
                    // Public job-offer browse; POST stays authenticated (authority in JobOfferService).
                    .requestMatchers(HttpMethod.GET, "/api/job-offers", "/api/job-offers/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/job-offers/*/apply").hasRole("CANDIDATE")
                    .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                    .anyRequest().authenticated()
                )
                // Before UsernamePasswordAuthenticationFilter so the SecurityContext is populated
                // from the JWT before any username/password handling runs.
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
