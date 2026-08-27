package dev.sabti.alumni_connect.employment;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface EmploymentEntryRepository extends JpaRepository<EmploymentEntry, Long> {
    List<EmploymentEntry> findByCandidateProfileOrderByStartedAtDesc(CandidateProfile profile);

    Optional<EmploymentEntry> findByIdAndCandidateProfile(Long id, CandidateProfile profile);

    // Open entries that have gone stale and haven't been chased recently.
    @Query("""
            select e from EmploymentEntry e
            where e.endedAt is null
              and (e.lastConfirmedAt is null or e.lastConfirmedAt < :staleBefore)
              and (e.lastNudgedAt is null or e.lastNudgedAt < :staleBefore)
            """)
    List<EmploymentEntry> findStaleOpenEntries(@Param("staleBefore") LocalDateTime staleBefore);
}
