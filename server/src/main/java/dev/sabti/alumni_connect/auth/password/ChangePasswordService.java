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

    // Returns exactly one thing: whether the supplied old password matched. That's the only
    // failure the caller can produce — the user is guaranteed to exist here (email comes from
    // the authenticated principal, and JwtRequestFilter already loaded it to authenticate the
    // request), so a missing user is an invariant violation, not a client error: throw (500)
    // rather than fold it into the false/400 path and mislabel it as a wrong password.
    @Transactional
    public boolean changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal has no matching user: " + email));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            log.warn("Change password failed: incorrect old password for email {}", email);
            return false;
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        return true;
    }
}
