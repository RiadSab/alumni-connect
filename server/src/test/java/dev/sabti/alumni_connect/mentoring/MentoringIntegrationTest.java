package dev.sabti.alumni_connect.mentoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sabti.alumni_connect.alumni.AlumniRecord;
import dev.sabti.alumni_connect.alumni.AlumniRecordRepository;
import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The loop against a real Postgres: a graduate publishes, a student books, the cap bites, and
// cancelling puts the hour back in the pool.
@SpringBootTest(properties = {
        "app.mentoring.bookings-per-semester=2",
        "app.mentoring.cron.confirm=-",
        "app.mentoring.cron.release=-",
        "app.mentoring.cron.remind=-"
})
@AutoConfigureMockMvc
@Testcontainers
class MentoringIntegrationTest {

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
    @Autowired private MentoringSlotRepository slots;
    @Autowired private MentoringBookingRepository bookings;

    private final ObjectMapper json = new ObjectMapper();

    private static final String PASSWORD = "s3cret-pw";
    private static final String MENTOR = "mentor@example.com";
    private static final String STUDENT = "student@example.com";
    private static final String OUTSIDER = "outsider@example.com";

    @BeforeEach
    void people() throws Exception {
        register(MENTOR, "Yasmine", "Alaoui", false);
        register(STUDENT, "Omar", "Idrissi", true);
        register(OUTSIDER, "Sara", "Bennani", false);

        // Only the mentor is on the school's roster.
        if (records.count() == 0) {
            User mentor = users.findByEmail(MENTOR).orElseThrow();
            AlumniRecord record = new AlumniRecord();
            record.setStudentId("2401");
            record.setFirstName("Yasmine");
            record.setLastName("Alaoui");
            record.setFieldOfStudy(Fields.COMPUTER_SCIENCE);
            record.setPromotionYear(2024);
            record.setClaimedBy(mentor);
            records.save(record);
        }
        // Rebuilt per test: bookings first, they hold the slots by foreign key.
        bookings.deleteAll();
        slots.deleteAll();
    }

    private void register(String email, String first, String last, boolean student) throws Exception {
        if (users.findByEmail(email).isPresent()) return;
        mvc.perform(post("/api/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s", "lastName": "%s",
                                  "email": "%s", "password": "%s",
                                  "phoneNumber": "0600000000", "isStudent": %s,
                                  "studentId": "%s",
                                  "fieldOfStudy": "COMPUTER_SCIENCE", "graduationYear": 2024
                                }
                                """.formatted(first, last, email, PASSWORD, student, email.split("@")[0])))
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
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long publish(String token, int daysAhead) throws Exception {
        String startsAt = OffsetDateTime.now().plusDays(daysAhead)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        var result = mvc.perform(post("/api/mentoring/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startsAt": "%s", "track": "DSA",
                                  "meetingLink": "https://meet.example.com/room-%s"
                                }
                                """.formatted(startsAt, daysAhead)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void onlyARosteredGraduateCanPublish() throws Exception {
        mvc.perform(post("/api/mentoring/slots")
                        .header("Authorization", "Bearer " + tokenFor(OUTSIDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "startsAt": "%s", "track": "DSA", "meetingLink": "https://meet.example.com/x" }
                                """.formatted(OffsetDateTime.now().plusDays(2)
                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
                .andExpect(status().isForbidden());
    }

    @Test
    void browsingNeverExposesTheMeetingLink() throws Exception {
        publish(tokenFor(MENTOR), 3);

        String body = mvc.perform(get("/api/mentoring/slots")
                        .header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].mentorName").value("Yasmine Alaoui"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("meet.example.com");
    }

    @Test
    void bookingTakesTheSlotAndCancellingGivesItBack() throws Exception {
        long slotId = publish(tokenFor(MENTOR), 4);
        String student = tokenFor(STUDENT);

        var booked = mvc.perform(post("/api/mentoring/slots/" + slotId + "/book")
                        .header("Authorization", "Bearer " + student))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("BOOKED"))
                // Four days out: the link is withheld until the join window opens.
                .andExpect(jsonPath("$.meetingLink").doesNotExist())
                .andReturn();
        long bookingId = json.readTree(booked.getResponse().getContentAsString()).get("id").asLong();

        // Gone from the pool, so nobody else sees it.
        mvc.perform(get("/api/mentoring/slots").header("Authorization", "Bearer " + student))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(delete("/api/mentoring/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + student))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/mentoring/slots").header("Authorization", "Bearer " + student))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void theSemesterCapStopsTheThirdBooking() throws Exception {
        String mentor = tokenFor(MENTOR);
        String student = tokenFor(STUDENT);
        long first = publish(mentor, 5);
        long second = publish(mentor, 6);
        long third = publish(mentor, 7);

        mvc.perform(post("/api/mentoring/slots/" + first + "/book").header("Authorization", "Bearer " + student))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/mentoring/slots/" + second + "/book").header("Authorization", "Bearer " + student))
                .andExpect(status().isCreated());

        // Cap is 2 for this test, so the third is refused — and the slot stays open for someone else.
        mvc.perform(post("/api/mentoring/slots/" + third + "/book").header("Authorization", "Bearer " + student))
                .andExpect(status().isConflict());

        assertThat(slots.findById(third).orElseThrow().getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    void aMentorCancellingTakesTheBookingWithIt() throws Exception {
        String mentor = tokenFor(MENTOR);
        long slotId = publish(mentor, 8);

        mvc.perform(post("/api/mentoring/slots/" + slotId + "/book")
                        .header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/mentoring/slots/" + slotId).header("Authorization", "Bearer " + mentor))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/mentoring/bookings/me").header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("CANCELLED_BY_MENTOR"));

        // The student is not charged for a session the mentor called off.
        mvc.perform(get("/api/mentoring/bookings/me/remaining").header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(jsonPath("$.remaining").value(2));
    }
}
