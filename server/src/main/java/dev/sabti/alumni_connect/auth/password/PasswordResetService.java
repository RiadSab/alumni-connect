package dev.sabti.alumni_connect.auth.password;

import dev.sabti.alumni_connect.auth.entities.PasswordResetToken;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.repositories.PasswordResetTokenRepository;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.shared.email.EmailSender;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

// Forgot/reset password flow. See docs/password-reset.md for the security rationale (no email
// enumeration, hashed single-use code, expiry, attempt cap).
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_TTL_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;
    // Returned for every reset failure so a caller can't tell why (wrong code vs no such request).
    private static final String RESET_FAILED = "Invalid or expired code";

    // Always succeeds from the caller's side (no email enumeration): a code is issued only for an
    // existing ACTIVE user, but the response is the same either way.
    @Transactional
    public void requestReset(ForgotPasswordDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElse(null);
        if (user == null || user.getUserStatus() != UserStatus.ACTIVE) {
            log.info("Password reset requested for unusable account {} — no code sent", dto.getEmail());
            return;
        }

        tokenRepository.deleteByUser(user);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCodeHash(passwordEncoder.encode(code));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        tokenRepository.save(token);

        emailSender.send(user.getEmail(), "Your password reset code",
                "Your password reset code is " + code + ". It expires in " + CODE_TTL_MINUTES + " minutes.");
    }

    @Transactional
    public void reset(ResetPasswordDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElse(null);
        if (user == null) throw new BadRequestException(RESET_FAILED);

        PasswordResetToken token = tokenRepository
                .findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new BadRequestException(RESET_FAILED));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) throw new BadRequestException(RESET_FAILED);
        if (token.getAttempts() >= MAX_ATTEMPTS) throw new BadRequestException(RESET_FAILED);

        if (!passwordEncoder.matches(dto.getCode(), token.getCodeHash())) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw new BadRequestException(RESET_FAILED);
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }
}
