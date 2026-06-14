package dev.sabti.alumni_connect.auth.password;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
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

    // The only failure the caller can produce is a wrong old password, now a 400 BadRequestException
    // with a caller-facing message (previously a bare false the controller mapped to an empty 400).
    // The user is guaranteed to exist here (email comes from the authenticated principal, and
    // JwtRequestFilter already loaded it to authenticate the request), so a missing user is an
    // invariant violation, not a client error: throw IllegalStateException (caught as 500 by the
    // global handler) rather than mislabel it as a wrong password.
    @Transactional
    public void changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal has no matching user: " + email));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            log.warn("Change password failed: incorrect old password for email {}", email);
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }
}
