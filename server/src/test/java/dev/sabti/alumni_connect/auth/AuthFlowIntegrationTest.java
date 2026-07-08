package dev.sabti.alumni_connect.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hs256-signing");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    private final ObjectMapper json = new ObjectMapper();

    private static final String EMAIL = "flow@example.com";
    private static final String PASSWORD = "s3cret-pw";

    @Test
    void registerLoginAccessRefresh() throws Exception {
        // 1. Register a candidate.
        mvc.perform(post("/api/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ada", "lastName": "Lovelace",
                                  "email": "%s", "password": "%s",
                                  "phoneNumber": "0600000000", "isStudent": false,
                                  "fieldOfStudy": "COMPUTER_SCIENCE", "graduationYear": 2024
                                }
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isCreated());

        var user = users.findByEmail(EMAIL).orElseThrow();
        user.setUserStatus(UserStatus.ACTIVE); // the user status must be flipped by an admin to be able to log in.
        users.save(user);

        // 2. Protected route with no token -> 401.
        mvc.perform(get("/api/candidates/me"))
                .andExpect(status().isUnauthorized());

        // 3. Login -> access token in body + refresh cookie set.
        var loginResult = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        JsonNode body = json.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = body.get("token").asText();
        Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");

        // 4. Same protected route, now with the Bearer token -> 200.
        mvc.perform(get("/api/candidates/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 5. Exchange the refresh cookie for a fresh access token.
        mvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"));
    }
}
