package dev.sabti.alumni_connect.auth.login;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;

// Role-agnostic login/refresh response: the access token plus the basic identity the client caches.
@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserType userType;

    public static LoginResponseDTO of(String token, User user) {
        return new LoginResponseDTO(token, user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getUserType());
    }
}
