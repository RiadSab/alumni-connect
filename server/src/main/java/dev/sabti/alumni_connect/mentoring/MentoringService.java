package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.alumni.AlumniRecordRepository;
import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.shared.exception.ConflictException;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import dev.sabti.alumni_connect.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static dev.sabti.alumni_connect.mentoring.BookingState.LIVE_STATES;

// Alumni publish interview slots, students book them. Supply is the scarce side, so the rules here
// protect the mentor's time: only verified graduates publish, and one student can't take the lot.
@Service
@RequiredArgsConstructor
public class MentoringService {
    private final MentoringSlotRepository slotRepository;
    private final MentoringBookingRepository bookingRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;
    private final AlumniRecordRepository alumniRecordRepository;
    private final MentoringSessionService sessionService;

    @Value("${app.mentoring.bookings-per-semester}")
    private int bookingsPerSemester;

    @Value("${app.mentoring.default-slot-minutes}")
    private int defaultSlotMinutes;

    @Value("${app.timezone}")
    private String timezone;

    // A booking the student still owns — these consume the semester quota. Cancelling before the
    // session gives the quota back; not turning up does not.
    private static final Set<BookingState> QUOTA_STATES =
            Set.of(BookingState.BOOKED, BookingState.CONFIRMED, BookingState.ATTENDED, BookingState.NO_SHOW);

    // Supply is volunteers' time. Miss two sessions and booking stops until someone looks at it.
    private static final int NO_SHOW_LIMIT = 2;

    private static final int MIN_MINUTES = 10;
    private static final int MAX_MINUTES = 90;

    // --- mentor side ---------------------------------------------------------

    @Transactional
    public MentoringSlotDTO publish(String email, PublishSlotDTO dto) {
        CandidateProfile mentor = mentorProfile(email);

        if (dto.getStartsAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("The slot must start in the future");
        }
        int minutes = dto.getDurationMinutes() == null ? defaultSlotMinutes : dto.getDurationMinutes();
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new BadRequestException("A session lasts between 10 and 90 minutes");
        }
        // The link is handed to a student and opened in their browser, so only plain https.
        String link = dto.getMeetingLink().trim();
        if (!link.startsWith("https://")) {
            throw new BadRequestException("The meeting link must start with https://");
        }

        MentoringSlot slot = new MentoringSlot();
        slot.setMentor(mentor);
        slot.setStartsAt(dto.getStartsAt());
        slot.setDurationMinutes(minutes);
        slot.setTrack(dto.getTrack());
        slot.setTargetField(dto.getTargetField());
        slot.setMeetingLink(link);
        slot.setStatus(SlotStatus.OPEN);
        return MentoringSlotDTO.from(slotRepository.save(slot));
    }

    @Transactional(readOnly = true)
    public List<MentoringSlotDTO> getMySlots(String email) {
        return slotRepository.findByMentorOrderByStartsAtAsc(mentorProfile(email)).stream()
                .map(MentoringSlotDTO::from)
                .toList();
    }

    @Transactional
    public void cancelSlot(String email, Long slotId) {
        MentoringSlot slot = slotRepository.findByIdAndMentor(slotId, mentorProfile(email))
                .orElseThrow(() -> new NotFoundException("Slot not found")); // not yours -> 404
        if (slot.getStatus() == SlotStatus.CANCELLED) return;

        // A booked slot takes its booking down with it; the student's quota is handed back.
        bookingRepository.findFirstBySlotAndStateIn(slot, LIVE_STATES).ifPresent(booking -> {
            booking.setState(BookingState.CANCELLED_BY_MENTOR);
            booking.setCancelledAt(LocalDateTime.now());
            bookingRepository.save(booking);
            sessionService.sessionCancelled(booking, true);
        });

        slot.setStatus(SlotStatus.CANCELLED);
        slotRepository.save(slot);
    }

    // --- student side --------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<MentoringSlotDTO> browse(MentoringTrack track, Fields field, Pageable pageable) {
        return slotRepository.findOpen(OffsetDateTime.now(), track, field, pageable)
                .map(MentoringSlotDTO::from);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MentoringBookingDTO book(String email, Long slotId) {
        CandidateProfile student = studentProfile(email);

        MentoringSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new NotFoundException("Slot not found"));
        OffsetDateTime now = OffsetDateTime.now();
        if (slot.getStatus() != SlotStatus.OPEN || !slot.getStartsAt().isAfter(now)) {
            throw new ConflictException("That slot is no longer available");
        }
        if (slot.getMentor().getId().equals(student.getId())) {
            throw new BadRequestException("You can't book your own slot");
        }

        // Counted against the semester the session falls in, not today's: booking August for
        // September must spend September's quota, or a student books next term's supply for free.
        if (bookingRepository.countByStudentAndState(student, BookingState.NO_SHOW) >= NO_SHOW_LIMIT) {
            throw new ForbiddenException("Booking is paused after two missed sessions — talk to the school");
        }

        long used = bookingsInSemesterOf(student, slot.getStartsAt());
        if (used >= bookingsPerSemester) {
            throw new ConflictException("You've used your " + bookingsPerSemester + " sessions for this semester");
        }

        slot.setStatus(SlotStatus.BOOKED);
        try {
            // Two students clicking at the same moment: @Version means the loser is told, not
            // silently given a slot somebody else already has.
            slotRepository.saveAndFlush(slot);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictException("That slot was just taken");
        }

        MentoringBooking booking = new MentoringBooking();
        booking.setSlot(slot);
        booking.setStudent(student);
        booking.setState(BookingState.BOOKED);
        if (!MentoringSessionService.hasTimeForConfirmation(slot.getStartsAt(), now)) {
            booking.setState(BookingState.CONFIRMED);
            booking.setConfirmedAt(now.toLocalDateTime());
        }
        booking = bookingRepository.save(booking);

        // Both sides get an email with a calendar invite; the mentor also gets an in-app notice.
        sessionService.sessionBooked(booking);
        return MentoringBookingDTO.forParticipant(booking);
    }

    // The link is only handed over inside the join window — see JoinWindow.
    @Transactional(readOnly = true)
    public List<MentoringBookingDTO> getMyBookings(String email) {
        return bookingRepository.findByStudentOrderByIdDesc(studentProfile(email)).stream()
                .map(MentoringBookingDTO::forParticipant)
                .toList();
    }

    // What the mentor sees: who booked which of their upcoming slots, and the attendance control.
    @Transactional(readOnly = true)
    public List<MentoringBookingDTO> getMentorBookings(String email) {
        return bookingRepository.findForMentor(mentorProfile(email), OffsetDateTime.now().minusDays(7)).stream()
                .map(MentoringBookingDTO::forParticipant)
                .toList();
    }

    // Attendance is the mentor's call, and only once the session has actually happened.
    @Transactional
    public void markAttendance(String email, Long bookingId, boolean attended) {
        CandidateProfile mentor = mentorProfile(email);
        MentoringBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!booking.getSlot().getMentor().getId().equals(mentor.getId())) {
            throw new NotFoundException("Booking not found"); // not yours -> 404
        }
        if (booking.getSlot().getStartsAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("The session hasn't happened yet");
        }
        if (!LIVE_STATES.contains(booking.getState())) {
            throw new ConflictException("That booking is already closed");
        }

        booking.setState(attended ? BookingState.ATTENDED : BookingState.NO_SHOW);
        bookingRepository.save(booking);
    }

    // What the booking page shows: the quota for the semester we are in now.
    @Transactional(readOnly = true)
    public long remainingBookings(String email) {
        CandidateProfile student = studentProfile(email);
        return Math.max(0, bookingsPerSemester - bookingsInSemesterOf(student, OffsetDateTime.now()));
    }

    @Transactional
    public void cancelBooking(String email, Long bookingId) {
        MentoringBooking booking = bookingRepository.findByIdAndStudent(bookingId, studentProfile(email))
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!LIVE_STATES.contains(booking.getState())) {
            throw new ConflictException("That booking is already closed");
        }

        booking.setState(BookingState.CANCELLED_BY_STUDENT);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
        sessionService.sessionCancelled(booking, false);

        // Back in the pool, so somebody else can still use the hour.
        MentoringSlot slot = booking.getSlot();
        if (slot.getStatus() == SlotStatus.BOOKED) {
            slot.setStatus(SlotStatus.OPEN);
            slotRepository.save(slot);
        }
    }

    // --- helpers -------------------------------------------------------------

    private long bookingsInSemesterOf(CandidateProfile student, OffsetDateTime moment) {
        ZoneId zone = ZoneId.of(timezone);
        LocalDate day = moment.atZoneSameInstant(zone).toLocalDate();

        // The Moroccan academic year splits in two: September to January, February to August.
        LocalDate from = day.getMonthValue() >= 9
                ? LocalDate.of(day.getYear(), 9, 1)
                : day.getMonthValue() <= 1
                        ? LocalDate.of(day.getYear() - 1, 9, 1)
                        : LocalDate.of(day.getYear(), 2, 1);
        LocalDate until = from.getMonthValue() == 9 ? from.plusMonths(5) : from.plusMonths(7);

        return bookingRepository.countInWindow(student, QUOTA_STATES,
                from.atStartOfDay(zone).toOffsetDateTime(),
                until.atStartOfDay(zone).toOffsetDateTime());
    }

    // Publishing is for graduates the school's own roster confirms.
    private CandidateProfile mentorProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Not a candidate"));
        if (!alumniRecordRepository.existsByClaimedBy(user)) {
            throw new ForbiddenException("Only a graduate on the school's roster can publish slots");
        }
        return candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException("Not a candidate"));
    }

    private CandidateProfile studentProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Not a candidate"));
        CandidateProfile profile = candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ForbiddenException("Not a candidate"));
        if (!Boolean.TRUE.equals(profile.getIsStudent())) {
            throw new ForbiddenException("Only students can book sessions");
        }
        return profile;
    }
}
