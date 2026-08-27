package dev.sabti.alumni_connect.employment;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmploymentEntryRepository extends JpaRepository<EmploymentEntry, Long> {
    List<EmploymentEntry> findByCandidateProfileOrderByStartedAtDesc(CandidateProfile profile);

    Optional<EmploymentEntry> findByIdAndCandidateProfile(Long id, CandidateProfile profile);
}
