package dev.sabti.alumni_connect.auth.login;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Optional<LoginResponseDTO> login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElse(null);
        if (user == null) {
            log.warn("Login failed: no user found with email {}", dto.getEmail());
            return Optional.empty();
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: incorrect password for email {}", dto.getEmail());
            return Optional.empty();
        }
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            log.warn("Login failed: user {} is not active (status: {})", dto.getEmail(), user.getUserStatus());
            return Optional.empty();
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return Optional.of(new LoginResponseDTO(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserType()
        ));
    }
}
