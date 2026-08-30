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

    Optional<MentoringBooking> findByConfirmTokenHash(String confirmTokenHash);

    long countByStudentAndState(CandidateProfile student, BookingState state);

    // Sessions the mentor is about to run (or just ran), newest first.
    @Query("""
            select b from MentoringBooking b
            where b.slot.mentor = :mentor and b.slot.startsAt > :after
            order by b.slot.startsAt asc
            """)
    List<MentoringBooking> findForMentor(@Param("mentor") CandidateProfile mentor,
                                         @Param("after") OffsetDateTime after);

    // Booked, still unasked: time to send the confirmation link.
    @Query("""
            select b from MentoringBooking b
            where b.state = dev.sabti.alumni_connect.mentoring.BookingState.BOOKED
              and b.confirmSentAt is null
              and b.slot.startsAt > :now and b.slot.startsAt <= :until
            """)
    List<MentoringBooking> findAwaitingConfirmation(@Param("now") OffsetDateTime now,
                                                    @Param("until") OffsetDateTime until);

    // Asked and ignored: the slot goes back in the pool.
    @Query("""
            select b from MentoringBooking b
            where b.state = dev.sabti.alumni_connect.mentoring.BookingState.BOOKED
              and b.confirmSentAt is not null
              and b.slot.startsAt <= :until
            """)
    List<MentoringBooking> findUnconfirmed(@Param("until") OffsetDateTime until);

    @Query("""
            select b from MentoringBooking b
            where b.state = dev.sabti.alumni_connect.mentoring.BookingState.CONFIRMED
              and b.remindedAt is null
              and b.slot.startsAt > :now and b.slot.startsAt <= :until
            """)
    List<MentoringBooking> findDueReminder(@Param("now") OffsetDateTime now,
                                           @Param("until") OffsetDateTime until);

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
