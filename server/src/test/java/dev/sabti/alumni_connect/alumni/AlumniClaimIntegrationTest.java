package dev.sabti.alumni_connect.alumni;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Invite -> claim link -> account, end to end against a real Postgres.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AlumniClaimIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hs256-signing");
        registry.add("app.frontend-url", () -> "https://alumni.example.com");
    }

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private AlumniRecordRepository records;
    @Autowired private AlumniClaimTokenRepository claimTokens;
    @Autowired private CandidateProfileRepository profiles;
    @Autowired private PasswordEncoder passwordEncoder;

    // Also keeps the test off a real SMTP server.
    @MockitoBean private EmailSender emailSender;

    private final ObjectMapper json = new ObjectMapper();

    private static final String ADMIN_EMAIL = "claim-admin@example.com";
    private static final String PASSWORD = "s3cret-pw";
    private static final Pattern CLAIM_LINK = Pattern.compile("/claim/([A-Za-z0-9_-]+)");

    @BeforeEach
    void reset() {
        claimTokens.deleteAll(); // tokens reference the records
        records.deleteAll();
        users.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            User admin = new User();
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
            admin.setFirstName("Root");
            admin.setLastName("Admin");
            admin.setPhoneNumber("0600000000");
            admin.setUserType(UserType.ADMINISTRATOR);
            admin.setUserStatus(UserStatus.ACTIVE);
            return users.save(admin);
        });
    }

    private AlumniRecord graduate(String studentId, String email) {
        AlumniRecord record = new AlumniRecord();
        record.setStudentId(studentId);
        record.setFirstName("Yasmine");
        record.setLastName("Alaoui");
        record.setFieldOfStudy(Fields.COMPUTER_SCIENCE);
        record.setPromotionYear(2024);
        record.setEmail(email);
        return records.save(record);
    }

    private String adminToken() throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(ADMIN_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    // The raw token only ever exists in the email body.
    private String inviteAndReadToken(String recipient) throws Exception {
        mvc.perform(post("/api/admin/alumni/invite?promotionYear=2024")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(1))
                .andExpect(jsonPath("$.skipped").value(0));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(org.mockito.ArgumentMatchers.eq(recipient), anyString(), body.capture());
        Matcher matcher = CLAIM_LINK.matcher(body.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    void inviteThenClaimCreatesAnActiveVerifiedAccount() throws Exception {
        graduate("2401", "yasmine@example.com");
        String token = inviteAndReadToken("yasmine@example.com");

        // The claim page reads the school's facts without any credentials.
        mvc.perform(get("/api/alumni/claim/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Yasmine"))
                .andExpect(jsonPath("$.promotionYear").value(2024))
                .andExpect(jsonPath("$.fieldOfStudy").value("COMPUTER_SCIENCE"));

        mvc.perform(post("/api/alumni/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "token": "%s", "password": "%s", "phoneNumber": "0600000001" }
                                """.formatted(token, PASSWORD)))
                .andExpect(status().isCreated());

        // Straight to ACTIVE: logging in works with no admin approval step.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "yasmine@example.com", "password": "%s" }
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk());

        User claimant = users.findByEmail("yasmine@example.com").orElseThrow();
        assertThat(claimant.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(claimant.getEmailVerified()).isTrue();

        // The promotion and field come from the roster, not from anything the person typed.
        var profile = profiles.findByUser(claimant).orElseThrow();
        assertThat(profile.getGraduationYear()).isEqualTo(2024);
        assertThat(profile.getFieldOfStudy()).isEqualTo(Fields.COMPUTER_SCIENCE);
        assertThat(profile.getStudentId()).isEqualTo("2401");

        assertThat(records.findByStudentId("2401").orElseThrow().getClaimedBy()).isNotNull();

        // Single use.
        mvc.perform(get("/api/alumni/claim/" + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void optOutKeepsTheRowAndDropsTheAddress() throws Exception {
        graduate("2402", "reda@example.com");
        String token = inviteAndReadToken("reda@example.com");

        mvc.perform(post("/api/alumni/claim/" + token + "/opt-out"))
                .andExpect(status().isNoContent());

        AlumniRecord record = records.findByStudentId("2402").orElseThrow();
        assertThat(record.getOptedOutAt()).isNotNull();
        assertThat(record.getEmail()).isNull();
        assertThat(records.count()).isEqualTo(1); // still counted in the denominator
    }

    @Test
    void unknownTokenIsRejected() throws Exception {
        mvc.perform(get("/api/alumni/claim/not-a-real-token"))
                .andExpect(status().isBadRequest());
    }
}
