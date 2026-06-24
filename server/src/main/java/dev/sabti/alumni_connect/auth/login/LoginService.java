package dev.sabti.alumni_connect.auth.login;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.auth.token.RefreshTokenService;
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
    private final RefreshTokenService refreshTokenService;

    // The access-token body plus the raw refresh token the controller sets as an httpOnly cookie.
    public record LoginResult(LoginResponseDTO response, String refreshToken) {}

    public Optional<LoginResult> login(LoginRequestDTO dto) {
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
        String refreshToken = refreshTokenService.issue(user);
        return Optional.of(new LoginResult(LoginResponseDTO.of(token, user), refreshToken));
    }
}
