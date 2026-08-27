package dev.sabti.alumni_connect.employment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.shared.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The whole employment loop against a real Postgres: an alumnus records a job, someone else can't
// touch it, a stale entry gets nudged, and the emailed link confirms it exactly once.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EmploymentIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hs256-signing");
    }

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private EmploymentEntryRepository entries;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private EmailSender emailSender;

    private final ObjectMapper json = new ObjectMapper();

    private static final String PASSWORD = "s3cret-pw";
    private static final String ALUMNUS = "alumnus@example.com";
    private static final String OTHER = "other@example.com";
    private static final String ADMIN = "employment-admin@example.com";

    @BeforeEach
    void accounts() throws Exception {
        registerCandidate(ALUMNUS, "Yasmine", "Alaoui");
        registerCandidate(OTHER, "Omar", "Idrissi");
        if (users.findByEmail(ADMIN).isEmpty()) {
            User admin = new User();
            admin.setEmail(ADMIN);
            admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
            admin.setFirstName("Root");
            admin.setLastName("Admin");
            admin.setPhoneNumber("0600000000");
            admin.setUserType(UserType.ADMINISTRATOR);
            admin.setUserStatus(UserStatus.ACTIVE);
            users.save(admin);
        }
    }

    private void registerCandidate(String email, String first, String last) throws Exception {
        if (users.findByEmail(email).isPresent()) return;
        mvc.perform(post("/api/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s", "lastName": "%s",
                                  "email": "%s", "password": "%s",
                                  "phoneNumber": "0600000000", "isStudent": false,
                                  "fieldOfStudy": "COMPUTER_SCIENCE", "graduationYear": 2024
                                }
                                """.formatted(first, last, email, PASSWORD)))
                .andExpect(status().isCreated());
        User user = users.findByEmail(email).orElseThrow();
        user.setUserStatus(UserStatus.ACTIVE);
        users.save(user);
    }

    private String tokenFor(String email) throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"%s\", \"password\": \"%s\" }".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private long createEntry(String accessToken) throws Exception {
        var result = mvc.perform(post("/api/employment/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "EMPLOYED", "employer": "Capgemini",
                                  "jobTitle": "Backend Developer", "sector": "IT",
                                  "city": "Casablanca", "startedAt": "2024-09-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employer").value("Capgemini"))
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void anEntryIsOwnedByItsAlumnus() throws Exception {
        String mine = tokenFor(ALUMNUS);
        long entryId = createEntry(mine);

        mvc.perform(get("/api/employment/me").header("Authorization", "Bearer " + mine))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].jobTitle").value("Backend Developer"));

        // Somebody else's entry is a 404, not a 403: ids aren't probeable.
        String theirs = tokenFor(OTHER);
        mvc.perform(patch("/api/employment/me/" + entryId)
                        .header("Authorization", "Bearer " + theirs)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"SEEKING\", \"startedAt\": \"2025-01-01\" }"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/employment/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void aStaleEntryIsNudgedAndConfirmedOnce() throws Exception {
        String mine = tokenFor(ALUMNUS);
        long entryId = createEntry(mine);

        // Age the entry past the one-year window the sweep looks for.
        EmploymentEntry entry = entries.findById(entryId).orElseThrow();
        entry.setLastConfirmedAt(LocalDateTime.now().minusMonths(13));
        entries.save(entry);

        mvc.perform(post("/api/admin/employment/nudge")
                        .header("Authorization", "Bearer " + tokenFor(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(1));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(ALUMNUS), anyString(), body.capture());
        assertThat(body.getValue()).contains("Backend Developer at Capgemini");

        Matcher matcher = Pattern.compile("/employment/confirm/([A-Za-z0-9_-]+)").matcher(body.getValue());
        assertThat(matcher.find()).isTrue();
        String confirmToken = matcher.group(1);

        mvc.perform(get("/api/employment/confirm/" + confirmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Yasmine"))
                .andExpect(jsonPath("$.employer").value("Capgemini"));

        mvc.perform(post("/api/employment/confirm/" + confirmToken))
                .andExpect(status().isNoContent());

        assertThat(entries.findById(entryId).orElseThrow().getLastConfirmedAt())
                .isAfter(LocalDateTime.now().minusMinutes(1));

        // Single use: the same link can't be replayed.
        mvc.perform(post("/api/employment/confirm/" + confirmToken))
                .andExpect(status().isBadRequest());
    }
}
