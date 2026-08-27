package dev.sabti.alumni_connect.employment;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.shared.email.EmailSender;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

// The yearly "is this still current?" nudge, and the one-time link that answers it.
@Service
@Slf4j
@RequiredArgsConstructor
public class EmploymentNudgeService {
    private final EmploymentEntryRepository employmentEntryRepository;
    private final EmploymentConfirmTokenRepository tokenRepository;
    private final EmailSender emailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int STALE_MONTHS = 12;
    private static final int TOKEN_TTL_DAYS = 60;
    // Same message for every failure so a caller can't probe which links exist.
    private static final String LINK_FAILED = "This link is invalid or has expired";

    // Runs weekly but only picks up entries untouched for a year, which makes it one nudge a year
    // per person, spread out instead of a single annual blast.
    @Scheduled(cron = "0 0 9 * * MON")
    public void scheduledSweep() {
        int sent = sendNudges();
        if (sent > 0) log.info("Employment nudge sweep: {} sent", sent);
    }

    @Transactional
    public int sendNudges() {
        List<EmploymentEntry> stale =
                employmentEntryRepository.findStaleOpenEntries(LocalDateTime.now().minusMonths(STALE_MONTHS));
        int sent = 0;
        for (EmploymentEntry entry : stale) {
            User user = entry.getCandidateProfile().getUser();
            try {
                String rawToken = issue(entry);
                emailSender.send(user.getEmail(), "Is your Alumni Connect profile still up to date?",
                        body(user, entry, rawToken));
                entry.setLastNudgedAt(LocalDateTime.now());
                employmentEntryRepository.save(entry);
                sent++;
            } catch (Exception e) {
                log.warn("Could not nudge employment entry {}: {}", entry.getId(), e.getMessage());
            }
        }
        return sent;
    }

    @Transactional(readOnly = true)
    public EmploymentConfirmDetailsDTO getDetails(String rawToken) {
        EmploymentEntry entry = validate(rawToken).getEmploymentEntry();
        return new EmploymentConfirmDetailsDTO(entry.getCandidateProfile().getUser().getFirstName(),
                entry.getStatus(), entry.getEmployer(), entry.getJobTitle(), entry.getStartedAt());
    }

    @Transactional
    public void confirm(String rawToken) {
        EmploymentConfirmToken token = validate(rawToken);
        LocalDateTime now = LocalDateTime.now();
        EmploymentEntry entry = token.getEmploymentEntry();
        entry.setLastConfirmedAt(now);
        employmentEntryRepository.save(entry);
        token.setUsedAt(now);
        tokenRepository.save(token);
    }

    private String issue(EmploymentEntry entry) {
        String raw = generateRawToken();
        EmploymentConfirmToken token = new EmploymentConfirmToken();
        token.setEmploymentEntry(entry);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(LocalDateTime.now().plusDays(TOKEN_TTL_DAYS));
        tokenRepository.save(token);
        return raw;
    }

    private EmploymentConfirmToken validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new BadRequestException(LINK_FAILED);
        EmploymentConfirmToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException(LINK_FAILED));
        if (token.getUsedAt() != null) throw new BadRequestException(LINK_FAILED);
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) throw new BadRequestException(LINK_FAILED);
        return token;
    }

    private String body(User user, EmploymentEntry entry, String rawToken) {
        return "Hi " + user.getFirstName() + ",\n\n"
                + "Your alumni profile says: " + summary(entry) + ".\n\n"
                + "Still right? Confirm it here:\n"
                + frontendUrl + "/employment/confirm/" + rawToken + "\n\n"
                + "If it has changed, sign in and update it:\n"
                + frontendUrl + "/profile/employment";
    }

    private String summary(EmploymentEntry entry) {
        return switch (entry.getStatus()) {
            case EMPLOYED -> entry.getJobTitle() + " at " + entry.getEmployer() + ", since " + entry.getStartedAt();
            case STUDYING -> "studying, since " + entry.getStartedAt();
            case SEEKING -> "looking for a role, since " + entry.getStartedAt();
        };
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
