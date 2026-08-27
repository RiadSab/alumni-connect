package dev.sabti.alumni_connect.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sabti.alumni_connect.alumni.AlumniRecord;
import dev.sabti.alumni_connect.alumni.AlumniRecordRepository;
import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.employment.EmploymentEntry;
import dev.sabti.alumni_connect.employment.EmploymentEntryRepository;
import dev.sabti.alumni_connect.employment.EmploymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A promotion of six with four answers, built straight in the database, then read back through
// the endpoint. The point of the fixture is that the two denominators disagree: 2 of 6 graduates
// are employed, but 2 of the 4 who answered are — 33% vs 50%, and only one of those is honest.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EmploymentReportIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hs256-signing");
    }

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CandidateProfileRepository profiles;
    @Autowired private AlumniRecordRepository records;
    @Autowired private EmploymentEntryRepository entries;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    private static final int PROMOTION = 2024;
    private static final String ADMIN = "report-admin@example.com";
    private static final String PASSWORD = "s3cret-pw";

    @BeforeEach
    void promotionOfSix() {
        if (records.count() > 0) return;

        // Employed since November 2024: four months after the July reference point.
        employedAlumnus("2401", "Capgemini", LocalDate.of(2024, 11, 1), null);
        // Employed since July 2025: twelve months.
        employedAlumnus("2402", "Capgemini", LocalDate.of(2025, 7, 1), null);
        // Still looking.
        alumnusWithEntry("2403", EmploymentStatus.SEEKING, null, LocalDate.of(2024, 9, 1), null);
        // A job that ended and nothing since: they answered, but we don't know where they are now.
        employedAlumnus("2404", "Atos", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 1));
        // Claimed the account and never filled anything in.
        claimedAlumnus("2405");
        // Never claimed: in the denominator, silent.
        roster("2406", null);

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

    private AlumniRecord roster(String studentId, User claimedBy) {
        AlumniRecord record = new AlumniRecord();
        record.setStudentId(studentId);
        record.setFirstName("Alum");
        record.setLastName(studentId);
        record.setFieldOfStudy(Fields.COMPUTER_SCIENCE);
        record.setPromotionYear(PROMOTION);
        record.setClaimedBy(claimedBy);
        return records.save(record);
    }

    private CandidateProfile claimedAlumnus(String studentId) {
        User user = new User();
        user.setEmail(studentId + "@example.com");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFirstName("Alum");
        user.setLastName(studentId);
        user.setPhoneNumber("0600000000");
        user.setUserType(UserType.CANDIDATE);
        user.setUserStatus(UserStatus.ACTIVE);
        user = users.save(user);

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(user);
        profile.setStudentId(studentId);
        profile.setFieldOfStudy(Fields.COMPUTER_SCIENCE);
        profile.setGraduationYear(PROMOTION);
        profile = profiles.save(profile);

        roster(studentId, user);
        return profile;
    }

    private void alumnusWithEntry(String studentId, EmploymentStatus status, String employer,
                                  LocalDate startedAt, LocalDate endedAt) {
        CandidateProfile profile = claimedAlumnus(studentId);
        EmploymentEntry entry = new EmploymentEntry();
        entry.setCandidateProfile(profile);
        entry.setStatus(status);
        entry.setEmployer(employer);
        entry.setJobTitle(employer == null ? null : "Developer");
        entry.setStartedAt(startedAt);
        entry.setEndedAt(endedAt);
        entries.save(entry);
    }

    private void employedAlumnus(String studentId, String employer, LocalDate startedAt, LocalDate endedAt) {
        alumnusWithEntry(studentId, EmploymentStatus.EMPLOYED, employer, startedAt, endedAt);
    }

    private String adminToken() throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"%s\", \"password\": \"%s\" }".formatted(ADMIN, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    @Test
    void theReportDividesByTheAnswersNotByThePromotion() throws Exception {
        mvc.perform(get("/api/admin/reports/employment?promotionYear=" + PROMOTION))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/admin/reports/employment?promotionYear=" + PROMOTION)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGraduates").value(6))
                .andExpect(jsonPath("$.claimed").value(5))
                .andExpect(jsonPath("$.responded").value(4))
                .andExpect(jsonPath("$.employed").value(2))
                .andExpect(jsonPath("$.seeking").value(1))
                .andExpect(jsonPath("$.studying").value(0))
                .andExpect(jsonPath("$.noCurrentPeriod").value(1))
                // 2 of the 4 who answered, not 2 of the 6 on the list.
                .andExpect(jsonPath("$.employmentRate").value(0.5))
                .andExpect(jsonPath("$.responseRate").value(4.0 / 6))
                // First jobs at 4, 12 and 2 months -> median 4.
                .andExpect(jsonPath("$.medianMonthsToFirstJob").value(4))
                .andExpect(jsonPath("$.topEmployers[0].employer").value("Capgemini"))
                .andExpect(jsonPath("$.topEmployers[0].count").value(2))
                .andExpect(jsonPath("$.topEmployers[1].employer").value("Atos"));
    }

    @Test
    void promotionYearsAreListedForThePicker() throws Exception {
        mvc.perform(get("/api/admin/reports/promotions")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(PROMOTION));
    }
}
