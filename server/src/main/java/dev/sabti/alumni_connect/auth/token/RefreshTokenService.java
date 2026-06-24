package dev.sabti.alumni_connect.auth.token;

import dev.sabti.alumni_connect.auth.entities.RefreshToken;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

// Issues, rotates, and revokes stateful refresh tokens; only the SHA-256 hash is stored.
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    static final int REFRESH_TTL_DAYS = 14;

    public record Rotation(User user, String rawToken) {}

    @Transactional
    public String issue(User user) {
        String raw = generateRawToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TTL_DAYS));
        repository.save(token);
        return raw;
    }

    // Validates and rotates the token (revoke old, issue new); empty = treat the caller as logged out.
    @Transactional
    public Optional<Rotation> rotate(String rawToken) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken)).orElse(null);
        if (token == null) return Optional.empty();
        if (token.getRevokedAt() != null) {
            // A revoked token resurfacing means it was likely stolen — drop the whole family.
            log.warn("Refresh token reuse detected for user {} — revoking all tokens", token.getUser().getId());
            revokeAll(token.getUser());
            return Optional.empty();
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) return Optional.empty();

        token.setRevokedAt(LocalDateTime.now());
        repository.save(token);
        return Optional.of(new Rotation(token.getUser(), issue(token.getUser())));
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                repository.save(token);
            }
        });
    }

    private void revokeAll(User user) {
        LocalDateTime now = LocalDateTime.now();
        for (RefreshToken token : repository.findByUserAndRevokedAtIsNull(user)) {
            token.setRevokedAt(now);
            repository.save(token);
        }
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
