package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserType;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.shared.email.EmailSender;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.shared.exception.ConflictException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// link() is the admin fallback for graduates the school has no address for.
@ExtendWith(MockitoExtension.class)
class AlumniClaimServiceTest {

    @Mock private AlumniRecordRepository alumniRecordRepository;
    @Mock private AlumniClaimTokenRepository claimTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailSender emailSender;
    @InjectMocks private AlumniClaimService service;

    private AlumniRecord record(long id) {
        AlumniRecord record = new AlumniRecord();
        record.setId(id);
        record.setStudentId("2401");
        record.setFieldOfStudy(Fields.DATA_SCIENCE);
        record.setPromotionYear(2024);
        return record;
    }

    private User user(long id, UserType type) {
        User user = new User();
        user.setId(id);
        user.setUserType(type);
        return user;
    }

    @Test
    void link_takesTheSchoolsFactsOverTheSelfDeclaredOnes() {
        AlumniRecord record = record(1L);
        User user = user(7L, UserType.CANDIDATE);
        CandidateProfile profile = new CandidateProfile();
        profile.setFieldOfStudy(Fields.ECONOMICS);   // what the person typed at registration
        profile.setGraduationYear(2019);

        when(alumniRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(alumniRecordRepository.existsByClaimedBy(user)).thenReturn(false);
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));

        service.link(1L, 7L);

        assertThat(profile.getFieldOfStudy()).isEqualTo(Fields.DATA_SCIENCE);
        assertThat(profile.getGraduationYear()).isEqualTo(2024);
        assertThat(profile.getStudentId()).isEqualTo("2401");
        assertThat(record.getClaimedBy()).isSameAs(user);
        assertThat(record.getClaimedAt()).isNotNull();
    }

    @Test
    void link_recordNotFound_throwsNotFound() {
        when(alumniRecordRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.link(1L, 7L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Alumni record not found");
    }

    @Test
    void link_alreadyClaimedRecord_throwsConflict() {
        AlumniRecord record = record(1L);
        record.setClaimedBy(user(9L, UserType.CANDIDATE));
        record.setClaimedAt(LocalDateTime.now());
        when(alumniRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.link(1L, 7L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This graduate is already linked to an account");
    }

    @Test
    void link_companyAccount_throwsBadRequest() {
        when(alumniRecordRepository.findById(1L)).thenReturn(Optional.of(record(1L)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, UserType.COMPANY_USER)));

        assertThatThrownBy(() -> service.link(1L, 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only a candidate account can be linked to a graduate");
    }

    @Test
    void link_accountAlreadyLinkedElsewhere_throwsConflict() {
        User user = user(7L, UserType.CANDIDATE);
        when(alumniRecordRepository.findById(1L)).thenReturn(Optional.of(record(1L)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(alumniRecordRepository.existsByClaimedBy(user)).thenReturn(true);

        assertThatThrownBy(() -> service.link(1L, 7L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("That account is already linked to another graduate");
    }

    @Test
    void claim_withAnUnknownToken_isRejected() {
        when(claimTokenRepository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        ClaimAccountDTO dto = new ClaimAccountDTO();
        dto.setToken("nope");
        dto.setPassword("s3cret-pw");

        assertThatThrownBy(() -> service.claim(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This link is invalid or has expired");
    }
}
