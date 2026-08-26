package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.shared.email.EmailSender;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.shared.exception.ConflictException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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

// Turns a roster row into a real account. See docs/alumni-roster.md.
@Slf4j
@Service
@RequiredArgsConstructor
public class AlumniClaimService {
    private final AlumniRecordRepository alumniRecordRepository;
    private final AlumniClaimTokenRepository claimTokenRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int CLAIM_TTL_DAYS = 30;

    // One message for every failure so a caller can't tell a wrong token from a used one.
    private static final String CLAIM_FAILED = "This link is invalid or has expired";

    @Transactional
    public ClaimInviteResultDTO invite(Integer promotionYear) {
        List<AlumniRecord> targets = promotionYear == null
                ? alumniRecordRepository.findByClaimedByIsNullAndOptedOutAtIsNullAndEmailIsNotNull()
                : alumniRecordRepository
                        .findByPromotionYearAndClaimedByIsNullAndOptedOutAtIsNullAndEmailIsNotNull(promotionYear);

        int sent = 0;
        for (AlumniRecord record : targets) {
            try {
                emailSender.send(record.getEmail(), "Claim your Alumni Connect account", inviteBody(record, issue(record)));
                sent++;
            } catch (Exception e) {
                // One bad address must not stop the rest of the promotion being invited.
                log.warn("Could not email a claim link for alumni record {}", record.getId(), e);
            }
        }
        return new ClaimInviteResultDTO(sent, targets.size() - sent);
    }

    @Transactional(readOnly = true)
    public AlumniClaimDetailsDTO getDetails(String rawToken) {
        AlumniRecord record = validate(rawToken).getAlumniRecord();
        return new AlumniClaimDetailsDTO(record.getFirstName(), record.getLastName(),
                record.getPromotionYear(), record.getFieldOfStudy(), record.getEmail());
    }

    @Transactional
    public void claim(ClaimAccountDTO dto) {
        AlumniClaimToken token = validate(dto.getToken());
        AlumniRecord record = token.getAlumniRecord();

        if (userRepository.findByEmail(record.getEmail()).isPresent()) {
            throw new ConflictException("An account already exists for " + record.getEmail());
        }

        User user = new User();
        user.setEmail(record.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(record.getFirstName());
        user.setLastName(record.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUserType(UserType.CANDIDATE);
        // No admin approval: the school's own list is the vetting, and clicking the link proves
        // the person reads that mailbox.
        user.setUserStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(user);
        profile.setIsStudent(false);
        profile.setStudentId(record.getStudentId());
        profile.setFieldOfStudy(record.getFieldOfStudy());
        profile.setGraduationYear(record.getPromotionYear());
        candidateProfileRepository.save(profile);

        markClaimed(record, user);
        token.setUsedAt(LocalDateTime.now());
        claimTokenRepository.save(token);
    }

    // "Remove me from the list": the row stays so the promotion's denominator is unchanged, but it
    // carries no address and is never emailed again.
    @Transactional
    public void optOut(String rawToken) {
        AlumniClaimToken token = validate(rawToken);
        AlumniRecord record = token.getAlumniRecord();
        record.setOptedOutAt(LocalDateTime.now());
        record.setEmail(null);
        alumniRecordRepository.save(record);

        token.setUsedAt(LocalDateTime.now());
        claimTokenRepository.save(token);
    }

    // Fallback for graduates the school has no address for: they register normally and an admin
    // links the account to their row.
    @Transactional
    public AlumniRecordDTO link(Long recordId, String email) {
        AlumniRecord record = alumniRecordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Alumni record not found"));
        if (record.getClaimedBy() != null) {
            throw new ConflictException("This graduate is already linked to an account");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No account with that email"));
        if (user.getUserType() != UserType.CANDIDATE) {
            throw new BadRequestException("Only a candidate account can be linked to a graduate");
        }
        if (alumniRecordRepository.existsByClaimedBy(user)) {
            throw new ConflictException("That account is already linked to another graduate");
        }

        CandidateProfile profile = candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("That account has no candidate profile"));
        // The school's list outranks what the person typed about themselves — that's what
        // linking is for.
        profile.setStudentId(record.getStudentId());
        profile.setFieldOfStudy(record.getFieldOfStudy());
        profile.setGraduationYear(record.getPromotionYear());
        candidateProfileRepository.save(profile);

        markClaimed(record, user);
        return AlumniRecordDTO.from(record);
    }

    private void markClaimed(AlumniRecord record, User user) {
        record.setClaimedBy(user);
        record.setClaimedAt(LocalDateTime.now());
        alumniRecordRepository.save(record);
    }

    private String issue(AlumniRecord record) {
        // One live link per graduate: re-inviting replaces the previous one.
        claimTokenRepository.deleteByAlumniRecord(record);

        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AlumniClaimToken token = new AlumniClaimToken();
        token.setAlumniRecord(record);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(LocalDateTime.now().plusDays(CLAIM_TTL_DAYS));
        claimTokenRepository.save(token);
        return raw;
    }

    private AlumniClaimToken validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException(CLAIM_FAILED);
        }
        AlumniClaimToken token = claimTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException(CLAIM_FAILED));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())
                || token.getAlumniRecord().getClaimedBy() != null) {
            throw new BadRequestException(CLAIM_FAILED);
        }
        return token;
    }

    private String inviteBody(AlumniRecord record, String rawToken) {
        return "Hello " + record.getFirstName() + ",\n\n"
                + "Your school listed you as a graduate of the " + record.getPromotionYear()
                + " promotion. Set a password to claim your Alumni Connect account:\n\n"
                + frontendUrl + "/claim/" + rawToken + "\n\n"
                + "The link works once and expires in " + CLAIM_TTL_DAYS + " days. "
                + "You received this because your school shared its graduate list with Alumni Connect; "
                + "the same page lets you remove yourself instead.";
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
