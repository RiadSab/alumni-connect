package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MentoringBookingRepository extends JpaRepository<MentoringBooking, Long> {
    List<MentoringBooking> findByStudentOrderByIdDesc(CandidateProfile student);

    Optional<MentoringBooking> findByIdAndStudent(Long id, CandidateProfile student);

    Optional<MentoringBooking> findFirstBySlotAndStateIn(MentoringSlot slot, Collection<BookingState> states);

    // The semester cap: bookings the student still owns, for slots inside the window.
    @Query("""
            select count(b) from MentoringBooking b
            where b.student = :student
              and b.state in :states
              and b.slot.startsAt >= :from
              and b.slot.startsAt < :until
            """)
    long countInWindow(@Param("student") CandidateProfile student,
                       @Param("states") Collection<BookingState> states,
                       @Param("from") OffsetDateTime from,
                       @Param("until") OffsetDateTime until);
}
