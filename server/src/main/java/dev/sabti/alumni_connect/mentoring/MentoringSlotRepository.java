package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MentoringSlotRepository extends JpaRepository<MentoringSlot, Long> {
    // What a student browses: still open, still in the future, optionally narrowed.
    @Query("""
            select s from MentoringSlot s
            where s.status = dev.sabti.alumni_connect.mentoring.SlotStatus.OPEN
              and s.startsAt > :after
              and (:track is null or s.track = :track)
              and (:field is null or s.targetField is null or s.targetField = :field)
            order by s.startsAt asc
            """)
    Page<MentoringSlot> findOpen(@Param("after") OffsetDateTime after,
                                 @Param("track") MentoringTrack track,
                                 @Param("field") Fields field,
                                 Pageable pageable);

    List<MentoringSlot> findByMentorOrderByStartsAtAsc(CandidateProfile mentor);

    Optional<MentoringSlot> findByIdAndMentor(Long id, CandidateProfile mentor);
}
