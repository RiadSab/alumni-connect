package dev.sabti.alumni_connect.employment;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// The alumnus owns these rows: nobody edits them on their behalf.
@Service
@RequiredArgsConstructor
public class EmploymentService {
    private final EmploymentEntryRepository employmentEntryRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;

    private static final LocalDate EARLIEST_START = LocalDate.of(1950, 1, 1);

    @Transactional(readOnly = true)
    public List<EmploymentEntryDTO> getMyEntries(String email) {
        return employmentEntryRepository.findByCandidateProfileOrderByStartedAtDesc(profileOf(email))
                .stream()
                .map(EmploymentEntryDTO::from)
                .toList();
    }

    @Transactional
    public EmploymentEntryDTO create(String email, SaveEmploymentEntryDTO dto) {
        validate(dto);
        EmploymentEntry entry = new EmploymentEntry();
        entry.setCandidateProfile(profileOf(email));
        apply(entry, dto);
        return EmploymentEntryDTO.from(employmentEntryRepository.save(entry));
    }

    @Transactional
    public EmploymentEntryDTO update(String email, Long id, SaveEmploymentEntryDTO dto) {
        validate(dto);
        EmploymentEntry entry = employmentEntryRepository.findByIdAndCandidateProfile(id, profileOf(email))
                .orElseThrow(() -> new NotFoundException("Employment entry not found")); // not yours -> 404
        apply(entry, dto);
        return EmploymentEntryDTO.from(employmentEntryRepository.save(entry));
    }

    @Transactional
    public void delete(String email, Long id) {
        EmploymentEntry entry = employmentEntryRepository.findByIdAndCandidateProfile(id, profileOf(email))
                .orElseThrow(() -> new NotFoundException("Employment entry not found"));
        employmentEntryRepository.delete(entry);
    }

    private void apply(EmploymentEntry entry, SaveEmploymentEntryDTO dto) {
        boolean employed = dto.getStatus() == EmploymentStatus.EMPLOYED;
        entry.setStatus(dto.getStatus());
        // Employer and title belong to a job; keeping them on a STUDYING row would skew "top employers".
        entry.setEmployer(employed ? dto.getEmployer().trim() : null);
        entry.setJobTitle(employed ? dto.getJobTitle().trim() : null);
        entry.setSector(blankToNull(dto.getSector()));
        entry.setCity(blankToNull(dto.getCity()));
        entry.setStartedAt(dto.getStartedAt());
        entry.setEndedAt(dto.getEndedAt());
        // Editing a row is itself a confirmation that it's current.
        entry.setLastConfirmedAt(LocalDateTime.now());
    }

    private void validate(SaveEmploymentEntryDTO dto) {
        if (dto.getStatus() == EmploymentStatus.EMPLOYED) {
            if (isBlank(dto.getEmployer())) throw new BadRequestException("An employer is required when employed");
            if (isBlank(dto.getJobTitle())) throw new BadRequestException("A job title is required when employed");
        }
        if (dto.getStartedAt().isBefore(EARLIEST_START)) {
            throw new BadRequestException("The start date is out of range");
        }
        if (dto.getEndedAt() != null && dto.getEndedAt().isBefore(dto.getStartedAt())) {
            throw new BadRequestException("The end date cannot be before the start date");
        }
    }

    private CandidateProfile profileOf(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Not a candidate"));
        return candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException("Not a candidate"));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
