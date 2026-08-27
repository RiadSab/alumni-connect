package dev.sabti.alumni_connect.employment;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmploymentServiceTest {

    @Mock private EmploymentEntryRepository employmentEntryRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private EmploymentService service;

    private static final String EMAIL = "alumnus@example.com";

    private CandidateProfile asCandidate() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        CandidateProfile profile = new CandidateProfile();
        profile.setId(7L);
        profile.setUser(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        return profile;
    }

    private SaveEmploymentEntryDTO employedAt(String employer, String title) {
        SaveEmploymentEntryDTO dto = new SaveEmploymentEntryDTO();
        dto.setStatus(EmploymentStatus.EMPLOYED);
        dto.setEmployer(employer);
        dto.setJobTitle(title);
        dto.setStartedAt(LocalDate.of(2024, 9, 1));
        return dto;
    }

    @Test
    void create_notACandidate_throwsForbidden() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(EMAIL, employedAt("Capgemini", "Developer")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not a candidate");
    }

    @Test
    void create_employedWithoutAnEmployer_throwsBadRequest() {
        assertThatThrownBy(() -> service.create(EMAIL, employedAt("  ", "Developer")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An employer is required when employed");
        verify(employmentEntryRepository, never()).save(any());
    }

    @Test
    void create_employedWithoutAJobTitle_throwsBadRequest() {
        assertThatThrownBy(() -> service.create(EMAIL, employedAt("Capgemini", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A job title is required when employed");
    }

    @Test
    void create_endBeforeStart_throwsBadRequest() {
        SaveEmploymentEntryDTO dto = employedAt("Capgemini", "Developer");
        dto.setEndedAt(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.create(EMAIL, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The end date cannot be before the start date");
    }

    @Test
    void create_employed_storesTheJobAndStampsAConfirmation() {
        asCandidate();
        when(employmentEntryRepository.save(any(EmploymentEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveEmploymentEntryDTO dto = employedAt(" Capgemini ", " Developer ");
        dto.setCity("Casablanca");

        EmploymentEntryDTO result = service.create(EMAIL, dto);

        assertThat(result.employer()).isEqualTo("Capgemini");
        assertThat(result.jobTitle()).isEqualTo("Developer");
        assertThat(result.city()).isEqualTo("Casablanca");
        assertThat(result.endedAt()).isNull();
        assertThat(result.lastConfirmedAt()).isNotNull();
    }

    // Otherwise a year of study would show up in "top employers".
    @Test
    void create_studying_dropsTheEmployerAndTitle() {
        asCandidate();
        when(employmentEntryRepository.save(any(EmploymentEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveEmploymentEntryDTO dto = employedAt("Capgemini", "Developer");
        dto.setStatus(EmploymentStatus.STUDYING);

        EmploymentEntryDTO result = service.create(EMAIL, dto);

        assertThat(result.status()).isEqualTo(EmploymentStatus.STUDYING);
        assertThat(result.employer()).isNull();
        assertThat(result.jobTitle()).isNull();
    }

    @Test
    void update_someoneElsesEntry_throwsNotFound() {
        CandidateProfile profile = asCandidate();
        when(employmentEntryRepository.findByIdAndCandidateProfile(5L, profile)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(EMAIL, 5L, employedAt("Capgemini", "Developer")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Employment entry not found");
    }

    @Test
    void delete_someoneElsesEntry_throwsNotFound() {
        CandidateProfile profile = asCandidate();
        when(employmentEntryRepository.findByIdAndCandidateProfile(5L, profile)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(EMAIL, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Employment entry not found");
        verify(employmentEntryRepository, never()).delete(any());
    }
}
