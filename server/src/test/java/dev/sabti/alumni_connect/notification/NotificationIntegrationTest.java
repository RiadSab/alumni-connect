package dev.sabti.alumni_connect.notification;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Reading and clearing notifications: unread drives the dashboard, the full list keeps history,
// and one user can never touch another's.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hs256-signing");
    }

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private NotificationRepository notifications;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    private static final String PASSWORD = "s3cret-pw";
    private static final String MINE = "reader@example.com";
    private static final String THEIRS = "stranger@example.com";

    @BeforeEach
    void seed() {
        candidate(MINE);
        candidate(THEIRS);
        // Rebuilt for every test: read-all in one would otherwise decide what the next one sees.
        notifications.deleteAll();
        notify(MINE, NotificationType.APPLICATION_ACCEPTED, "Backend Engineer");
        notify(MINE, NotificationType.INTERVIEW_SCHEDULED, "Data Analyst");
        notify(THEIRS, NotificationType.APPLICATION_REJECTED, "Not yours");
    }

    private User candidate(String email) {
        return users.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(PASSWORD));
            user.setFirstName("Test");
            user.setLastName("User");
            user.setPhoneNumber("0600000000");
            user.setUserType(UserType.CANDIDATE);
            user.setUserStatus(UserStatus.ACTIVE);
            return users.save(user);
        });
    }

    private void notify(String email, NotificationType type, String subject) {
        Notification notification = new Notification();
        notification.setUser(candidate(email));
        notification.setType(type);
        notification.setSubject(subject);
        notification.setContext("Acme");
        notification.setLink("/applications/1");
        notifications.save(notification);
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

    private long idOfFirstUnread(String accessToken) throws Exception {
        var result = mvc.perform(get("/api/notifications/unread")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    @Test
    void readingOneRemovesItFromUnreadButKeepsItInTheList() throws Exception {
        String token = tokenFor(MINE);

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        long id = idOfFirstUnread(token);

        mvc.perform(post("/api/notifications/" + id + "/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // Still in the history, now stamped as read.
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].readAt").isNotEmpty());

        assertThat(notifications.findById(id).orElseThrow().getReadAt()).isNotNull();
    }

    @Test
    void oneUserCannotReadOrClearAnother() throws Exception {
        String stranger = tokenFor(THEIRS);
        long mine = idOfFirstUnread(tokenFor(MINE));

        // Somebody else's notification is a 404, not a 403.
        mvc.perform(post("/api/notifications/" + mine + "/read")
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + stranger))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].subject").value("Not yours"));

        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void readAllClearsWhatIsLeft() throws Exception {
        String token = tokenFor(MINE);

        mvc.perform(post("/api/notifications/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}
