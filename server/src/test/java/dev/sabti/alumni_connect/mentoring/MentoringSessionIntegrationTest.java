package dev.sabti.alumni_connect.mentoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sabti.alumni_connect.alumni.AlumniRecord;
import dev.sabti.alumni_connect.alumni.AlumniRecordRepository;
import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.shared.email.EmailAttachment;
import dev.sabti.alumni_connect.shared.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The lifecycle around a booked session: the invite that lands in a calendar, the confirmation
// asked a day out, the release of what nobody confirmed, and the join window.
@SpringBootTest(properties = {
        "app.mentoring.cron.confirm=-",
        "app.mentoring.cron.release=-",
        "app.mentoring.cron.remind=-"
})
@AutoConfigureMockMvc
@Testcontainers
class MentoringSessionIntegrationTest {

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
    @Autowired private MentoringSlotRepository slots;
    @Autowired private MentoringBookingRepository bookings;
    @Autowired private MentoringSessionService sessionService;

    @MockitoBean private EmailSender emailSender;

    private final ObjectMapper json = new ObjectMapper();

    private static final String PASSWORD = "s3cret-pw";
    private static final String MENTOR = "session-mentor@example.com";
    private static final String STUDENT = "session-student@example.com";

    @BeforeEach
    void people() throws Exception {
        register(MENTOR, "Yasmine", "Alaoui", false);
        register(STUDENT, "Omar", "Idrissi", true);
        if (records.count() == 0) {
            AlumniRecord record = new AlumniRecord();
            record.setStudentId("2401");
            record.setFirstName("Yasmine");
            record.setLastName("Alaoui");
            record.setFieldOfStudy(Fields.COMPUTER_SCIENCE);
            record.setPromotionYear(2024);
            record.setClaimedBy(users.findByEmail(MENTOR).orElseThrow());
            records.save(record);
        }
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

    private record Session(long slotId, long bookingId) {}

    private Session bookSlotStartingIn(long minutes) throws Exception {
        String startsAt = OffsetDateTime.now().plusMinutes(minutes)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        var published = mvc.perform(post("/api/mentoring/slots")
                        .header("Authorization", "Bearer " + tokenFor(MENTOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "startsAt": "%s", "track": "DSA", "meetingLink": "https://meet.example.com/room" }
                                """.formatted(startsAt)))
                .andExpect(status().isCreated())
                .andReturn();
        long slotId = json.readTree(published.getResponse().getContentAsString()).get("id").asLong();

        var booked = mvc.perform(post("/api/mentoring/slots/" + slotId + "/book")
                        .header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(status().isCreated())
                .andReturn();
        long bookingId = json.readTree(booked.getResponse().getContentAsString()).get("id").asLong();
        return new Session(slotId, bookingId);
    }

    @Test
    void bookingSendsBothSidesACalendarInvite() throws Exception {
        bookSlotStartingIn(60 * 30);

        ArgumentCaptor<EmailAttachment> ics = ArgumentCaptor.forClass(EmailAttachment.class);
        verify(emailSender, atLeastOnce()).send(anyString(), anyString(), anyString(), ics.capture());

        String calendar = new String(ics.getAllValues().get(0).content(), StandardCharsets.UTF_8);
        assertThat(ics.getAllValues().get(0).filename()).isEqualTo("session.ics");
        assertThat(calendar).contains("BEGIN:VEVENT").contains("METHOD:REQUEST")
                .contains("LOCATION:https://meet.example.com/room");
        // One attendee per invite: neither party learns the other's address from it.
        assertThat(calendar.lines().filter(line -> line.startsWith("ATTENDEE")).count()).isEqualTo(1);
    }

    @Test
    void confirmingKeepsTheSlot() throws Exception {
        long bookingId = bookSlotStartingIn(60 * 20).bookingId();

        sessionService.askForConfirmations();

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(STUDENT), eq("Confirm your mock interview"), body.capture());
        Matcher matcher = Pattern.compile("/mentoring/confirm/([A-Za-z0-9_-]+)").matcher(body.getValue());
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        mvc.perform(get("/api/mentoring/confirm/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("BOOKED"))
                // The confirmation page shows the session, not the room address.
                .andExpect(jsonPath("$.meetingLink").doesNotExist());

        mvc.perform(post("/api/mentoring/confirm/" + token)).andExpect(status().isNoContent());

        assertThat(bookings.findById(bookingId).orElseThrow().getState()).isEqualTo(BookingState.CONFIRMED);

        // A confirmed session survives the release sweep.
        sessionService.releaseUnconfirmed();
        assertThat(bookings.findById(bookingId).orElseThrow().getState()).isEqualTo(BookingState.CONFIRMED);
    }

    @Test
    void ignoringTheConfirmationPutsTheSlotBack() throws Exception {
        Session session = bookSlotStartingIn(60 * 6);
        long bookingId = session.bookingId();
        sessionService.askForConfirmations();

        sessionService.releaseUnconfirmed();

        assertThat(bookings.findById(bookingId).orElseThrow().getState()).isEqualTo(BookingState.RELEASED_UNCONFIRMED);
        assertThat(slots.findById(session.slotId()).orElseThrow().getStatus()).isEqualTo(SlotStatus.OPEN);

        // Somebody else can have the hour.
        mvc.perform(get("/api/mentoring/slots").header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void theLinkArrivesTenMinutesBeforeTheSession() throws Exception {
        bookSlotStartingIn(5);

        mvc.perform(get("/api/mentoring/bookings/me").header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meetingLink").value("https://meet.example.com/room"));

        // The mentor's own view of the same session.
        mvc.perform(get("/api/mentoring/bookings/mentor").header("Authorization", "Bearer " + tokenFor(MENTOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Omar Idrissi"))
                .andExpect(jsonPath("$[0].meetingLink").value("https://meet.example.com/room"));
    }

    @Test
    void aMissedSessionIsRecordedByTheMentor() throws Exception {
        Session session = bookSlotStartingIn(30);
        long bookingId = session.bookingId();

        // Move the session into the past so attendance can be recorded.
        MentoringSlot slot = slots.findById(session.slotId()).orElseThrow();
        slot.setStartsAt(OffsetDateTime.now().minusHours(1));
        slots.save(slot);

        mvc.perform(post("/api/mentoring/bookings/" + bookingId + "/attendance?attended=false")
                        .header("Authorization", "Bearer " + tokenFor(MENTOR)))
                .andExpect(status().isNoContent());

        assertThat(bookings.findById(bookingId).orElseThrow().getState()).isEqualTo(BookingState.NO_SHOW);

        // A student can't mark their own attendance.
        mvc.perform(post("/api/mentoring/bookings/" + bookingId + "/attendance?attended=true")
                        .header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(status().isForbidden());
    }
}
