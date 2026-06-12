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

@Configuration // tell spring that this class contain bean definitions

//without it we wont have http security features
@EnableWebSecurity // activate security filter chain and web security config

@EnableMethodSecurity // to enable method level security annotations like @PreAuthorize, @Secured, @RolesAllowed

@AllArgsConstructor
public class SecurityConfig {
    private final JwtRequestFilter jwtRequestFilter;
    private final MyUserDetailsService myUserDetailsService;

    @Bean  // define a bean to be managed by spring container
    // spring search for a bean of type SecurityFilterChain, if found it uses it to configure security for HTTP requests
    // SecurityFilterChaine main security configuration for HTTP requests
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // set session management to stateless, no HTTP session is created or used by spring security
                .authorizeHttpRequests(auth -> auth
                    // A logged-in user changing their own password — must be declared BEFORE the
                    // broad /api/auth/** permitAll below, which otherwise (first match wins) would
                    // make this public. Everything else under /api/auth (login, register) is public.
                    .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()
                    .requestMatchers("/api/auth/**").permitAll()
                    // Public read access — feeds the "join an existing company" registration
                    // form, which runs before the registrant has an account. Scoped to GET only
                    // so future write endpoints on /api/companies/** stay behind authentication.
                    .requestMatchers(HttpMethod.GET, "/api/companies", "/api/companies/**").permitAll()
                    // Applicant lists are private to the posting company's own OWNER/RECRUITER —
                    // must be declared BEFORE the broad public job-offers GET matcher below,
                    // since /api/job-offers/** would otherwise also match this sub-resource
                    // and wrongly expose candidates' applications. Fine-grained "same company"
                    // authority is checked in JobApplicationService.
                    .requestMatchers(HttpMethod.GET, "/api/job-offers/*/applications").authenticated()
                    // "My company's postings" — must also be declared before the broad public
                    // matcher below, same reasoning as /*/applications: it's a private,
                    // company-scoped view, not a publicly browsable offer.
                    .requestMatchers(HttpMethod.GET, "/api/job-offers/me").authenticated()
                    // "Recommended for you" — candidate-only, ranked against the caller's own
                    // profile skills. Declared before the broad public GET below so it isn't made
                    // public; "must be a candidate" maps cleanly onto one role, like /*/apply.
                    .requestMatchers(HttpMethod.GET, "/api/job-offers/recommended").hasRole("CANDIDATE")
                    // Candidate self-profile — declared before /api/candidates/* below so
                    // "/me" isn't caught by the admin-only by-id matcher.
                    .requestMatchers(HttpMethod.GET, "/api/candidates/me").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/candidates/me").authenticated()
                    // Admin review of any candidate's profile by User id (e.g. from
                    // /api/admin/pending-users before approve/reject).
                    .requestMatchers(HttpMethod.GET, "/api/candidates/*").hasRole("ADMINISTRATOR")
                    // The caller's own company roster — authenticated; "is a company user" is
                    // checked in the service (a candidate gets 403). Exact path, distinct from
                    // the /me and /* matchers.
                    .requestMatchers(HttpMethod.GET, "/api/company-users").authenticated()
                    // Company-user self-profile — same ordering reasoning as /api/candidates/me.
                    .requestMatchers(HttpMethod.GET, "/api/company-users/me").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/company-users/me").authenticated()
                    // A company OWNER changing a member's role — only "authenticated" here; the
                    // "OWNER of the same company" authority is checked in CompanyUserService.
                    .requestMatchers(HttpMethod.PATCH, "/api/company-users/*/role").authenticated()
                    // Admin review of any company-user's profile by User id.
                    .requestMatchers(HttpMethod.GET, "/api/company-users/*").hasRole("ADMINISTRATOR")
                    // Public job-offer browsing — candidates search/apply before having an
                    // account. Scoped to GET only; POST (posting an offer) stays authenticated
                    // and is further authority-checked in JobOfferService.
                    .requestMatchers(HttpMethod.GET, "/api/job-offers", "/api/job-offers/**").permitAll()
                    // Unlike posting (OWNER/RECRUITER — too fine-grained for hasRole, checked
                    // in JobOfferService), "must be a candidate" maps exactly onto a single
                    // role, so it's safe and precise to gate it here too.
                    .requestMatchers(HttpMethod.POST, "/api/job-offers/*/apply").hasRole("CANDIDATE")
                    .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                    .anyRequest().authenticated()
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);  // add our custom JWT filter before the built-in UsernamePasswordAuthenticationFilter
                    // if we dont add it before, our filter will not be executed
                    // since UsernamePasswordAuthenticationFilter is responsible for processing authentication requests based on username and password,  it will not find them in this case
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
