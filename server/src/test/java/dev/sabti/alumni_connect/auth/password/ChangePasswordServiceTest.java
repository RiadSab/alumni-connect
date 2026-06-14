package dev.sabti.alumni_connect.auth.password;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private ChangePasswordService service;

    private static final String EMAIL = "user@example.com";

    private ChangePasswordDTO dto(String oldPw, String newPw) {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(oldPw);
        dto.setNewPassword(newPw);
        return dto;
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBadRequest() {
        User user = new User();
        user.setPasswordHash("stored-hash");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(EMAIL, dto("wrong", "new-pass")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    void changePassword_correctCurrentPassword_encodesAndSavesNewHash() {
        User user = new User();
        user.setPasswordHash("stored-hash");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        service.changePassword(EMAIL, dto("current", "new-pass"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_authenticatedPrincipalHasNoUser_throwsIllegalState() {
        // Invariant guard: the email comes from the authenticated principal, so a missing user is a
        // bug, not a client error -> IllegalStateException (surfaced as 500), not a wrong-password 400.
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword(EMAIL, dto("x", "y")))
                .isInstanceOf(IllegalStateException.class);
    }
}
