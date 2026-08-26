package dev.sabti.alumni_connect.alumni;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exercises the real chain against a real Postgres: V4 applied by Flyway, admin-only security,
// multipart upload, and the upsert on re-import.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AlumniImportIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hs256-signing");
    }

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private AlumniRecordRepository records;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    private static final String ADMIN_EMAIL = "roster-admin@example.com";
    private static final String PASSWORD = "s3cret-pw";

    private static final String ROSTER = """
            student_id,first_name,last_name,field_of_study,promotion_year,email
            2401,Yasmine,Alaoui,Computer Science,2024,yasmine@example.com
            2402,Mohammed,"El Fassi, Jr",Physics,2024,
            2403,Sara,Bennani,Astrology,2024,
            """;

    @BeforeEach
    void adminExists() {
        if (users.findByEmail(ADMIN_EMAIL).isPresent()) return;
        User admin = new User();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
        admin.setFirstName("Root");
        admin.setLastName("Admin");
        admin.setPhoneNumber("0600000000");
        admin.setUserType(UserType.ADMINISTRATOR);
        admin.setUserStatus(UserStatus.ACTIVE);
        users.save(admin);
    }

    private String adminToken() throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(ADMIN_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private MockMultipartFile roster() {
        return new MockMultipartFile("file", "roster.csv", "text/csv", ROSTER.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importRequiresAnAdmin() throws Exception {
        mvc.perform(multipart("/api/admin/alumni/import").file(roster()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void importThenReimportUpsertsAndReportsBadRows() throws Exception {
        String token = adminToken();

        mvc.perform(multipart("/api/admin/alumni/import").file(roster())
                        .param("dryRun", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.errors.length()").value(1));

        assertThat(records.findByStudentId("2401")).isEmpty(); // dry run wrote nothing

        mvc.perform(multipart("/api/admin/alumni/import").file(roster())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.errors[0].line").value(4));

        assertThat(records.findByStudentId("2402").orElseThrow().getLastName()).isEqualTo("El Fassi, Jr");

        // Re-uploading the same file updates in place instead of duplicating.
        mvc.perform(multipart("/api/admin/alumni/import").file(roster())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(2));

        assertThat(records.count()).isEqualTo(2);

        mvc.perform(get("/api/admin/alumni?promotionYear=2024")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].claimed").value(false));
    }
}
