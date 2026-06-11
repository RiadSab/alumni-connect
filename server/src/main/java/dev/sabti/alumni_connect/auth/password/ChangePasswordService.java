package dev.sabti.alumni_connect.auth.password;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // false signals "soft failure" — the supplied old password didn't match (the only
    // outcome the caller can act on). The user always exists: email comes from the
    // authenticated principal, so a missing user would be an inconsistent state, logged
    // and reported the same way rather than surfaced as a distinct status.
    @Transactional
    public boolean changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("Change password failed: no user found with email {}", email);
            return false;
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            log.warn("Change password failed: incorrect old password for email {}", email);
            return false;
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        return true;
    }
}
