package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.notification.NotificationService;
import dev.sabti.alumni_connect.notification.NotificationType;
import dev.sabti.alumni_connect.shared.email.EmailAttachment;
import dev.sabti.alumni_connect.shared.email.EmailSender;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

// Everything that happens around a booked session: the invitations, the 24h confirmation, the
// release of slots nobody confirmed, the hour-before reminder, and attendance.
@Service
@Slf4j
@RequiredArgsConstructor
public class MentoringSessionService {
    private final MentoringBookingRepository bookingRepository;
    private final MentoringSlotRepository slotRepository;
    private final EmailSender emailSender;
    private final NotificationService notificationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.timezone}")
    private String timezone;

    @Value("${app.mail.from}")
    private String mailFrom;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int CONFIRM_ASKED_HOURS = 24;
    private static final int RELEASE_LEAD_HOURS = 12;
    private static final int CONFIRMATION_GRACE_HOURS = 2;
    private static final int REBOOKING_BUFFER_HOURS = 1;
    private static final int REMINDER_MINUTES = 60;
    private static final String LINK_FAILED = "This link is invalid or has expired";
    private static final String STUDENT_SESSIONS_PATH = "/mentoring";
    private static final String MENTOR_SESSIONS_PATH = "/mentoring/slots";

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("EEEE d MMMM, HH:mm (OOOO)", Locale.ENGLISH);

    // --- the two events the booking flow raises -----------------------------

    @Transactional
    public void sessionBooked(MentoringBooking booking) {
        MentoringSlot slot = booking.getSlot();
        User student = booking.getStudent().getUser();
        User mentor = slot.getMentor().getUser();

        String when = when(slot);
        invite(student, "Your mock interview is booked",
                "Your session with " + name(mentor) + " is booked for " + when + ".\n\n"
                        + "The joining link appears on your session page ten minutes before we start:",
                STUDENT_SESSIONS_PATH, booking, false);
        invite(mentor, "A student booked one of your slots",
                name(student) + " booked your " + track(slot) + " slot on " + when + ".",
                MENTOR_SESSIONS_PATH, booking, false);

        notificationService.notify(mentor, NotificationType.MENTORING_SLOT_BOOKED,
                track(slot), name(student), MENTOR_SESSIONS_PATH);
    }

    @Transactional
    public void sessionCancelled(MentoringBooking booking, boolean byMentor) {
        MentoringSlot slot = booking.getSlot();
        User student = booking.getStudent().getUser();
        User mentor = slot.getMentor().getUser();
        String when = when(slot);

        // The cancellation invite removes the entry from the calendar it was added to.
        invite(student, "Your mock interview was cancelled",
                (byMentor ? name(mentor) + " cancelled" : "You cancelled") + " the session on " + when + "."
                        + (byMentor ? "\n\nYour session allowance was not used." : ""),
                STUDENT_SESSIONS_PATH, booking, true);
        invite(mentor, "A mock interview was cancelled",
                (byMentor ? "You cancelled" : name(student) + " cancelled") + " the session on " + when + ".",
                MENTOR_SESSIONS_PATH, booking, true);

        User other = byMentor ? student : mentor;
        notificationService.notify(other, NotificationType.MENTORING_SESSION_CANCELLED,
                track(slot), when, byMentor ? STUDENT_SESSIONS_PATH : MENTOR_SESSIONS_PATH);
    }

    // --- sweeps -------------------------------------------------------------

    // Hourly rather than daily: a session booked 20 hours ahead still gets asked to confirm.
    // Cron is a property so a test can switch the sweep off with "-": a sweep firing mid-test
    // would release the very bookings the test is asserting on.
    @Scheduled(cron = "${app.mentoring.cron.confirm:0 5 * * * *}")
    @Transactional
    public void askForConfirmations() {
        OffsetDateTime now = OffsetDateTime.now();
        for (MentoringBooking booking : bookingRepository.findAwaitingConfirmation(now, now.plusHours(CONFIRM_ASKED_HOURS))) {
            OffsetDateTime sentAt = OffsetDateTime.now();
            MentoringSlot slot = booking.getSlot();
            OffsetDateTime deadline = confirmationDeadline(slot.getStartsAt(), sentAt);
            if (!hasRebookingTime(slot.getStartsAt(), deadline)) {
                markConfirmed(booking, sentAt);
                continue;
            }
            try {
                byte[] bytes = new byte[TOKEN_BYTES];
                RANDOM.nextBytes(bytes);
                String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                emailSender.send(booking.getStudent().getUser().getEmail(),
                        "Confirm your mock interview",
                        "Your session with " + name(slot.getMentor().getUser())
                                + " is on " + when(slot) + ".\n\n"
                                + "Please confirm by " + formatTime(deadline) + " to keep your slot:\n"
                                + frontendUrl + "/mentoring/confirm/" + raw);
                booking.setConfirmTokenHash(hash(raw));
                booking.setConfirmSentAt(sentAt.toLocalDateTime());
                bookingRepository.save(booking);
            } catch (Exception e) {
                log.warn("Could not ask for confirmation on booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${app.mentoring.cron.release:0 10 * * * *}")
    @Transactional
    public void releaseUnconfirmed() {
        OffsetDateTime now = OffsetDateTime.now();
        for (MentoringBooking booking : bookingRepository.findUnconfirmed(now, now.plusHours(RELEASE_LEAD_HOURS))) {
            if (now.isBefore(confirmationDeadline(booking))) continue;

            MentoringSlot slot = booking.getSlot();
            // A delayed sweep should keep the appointment when it is too late to offer it again.
            if (!hasRebookingTime(slot.getStartsAt(), now)) {
                markConfirmed(booking, now);
                continue;
            }

            booking.setState(BookingState.RELEASED_UNCONFIRMED);
            booking.setCancelledAt(now.toLocalDateTime());
            bookingRepository.save(booking);

            // Back in the pool if there is still time for someone else to take it.
            if (slot.getStatus() == SlotStatus.BOOKED) {
                slot.setStatus(SlotStatus.OPEN);
                slotRepository.save(slot);
            }

            User student = booking.getStudent().getUser();
            String studentEmail = student.getEmail();
            String mentorEmail = slot.getMentor().getUser().getEmail();
            String when = when(slot);
            afterCommit(() -> {
                safeSend(studentEmail, "Your mock interview slot was released",
                        "You didn't confirm the session on " + when + ", so it went back to another student.\n\n"
                                + "You can book another one:\n" + frontendUrl + STUDENT_SESSIONS_PATH);
                safeSend(mentorEmail, "A slot was released",
                        "The student didn't confirm, so your " + when + " slot is open again.");
            });
            notificationService.notify(student, NotificationType.MENTORING_SESSION_RELEASED,
                    track(slot), when, STUDENT_SESSIONS_PATH);
        }
    }

    @Scheduled(cron = "${app.mentoring.cron.remind:0 */15 * * * *}")
    @Transactional
    public void sendReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        for (MentoringBooking booking : bookingRepository.findDueReminder(now, now.plusMinutes(REMINDER_MINUTES))) {
            MentoringSlot slot = booking.getSlot();
            String body = "Your mock interview starts at " + when(slot) + ".\n\n"
                    + "The joining link appears on your session page ten minutes before we start:\n";
            safeSend(booking.getStudent().getUser().getEmail(), "Your mock interview is in an hour",
                    body + frontendUrl + STUDENT_SESSIONS_PATH);
            safeSend(slot.getMentor().getUser().getEmail(), "Your mock interview is in an hour",
                    body + frontendUrl + MENTOR_SESSIONS_PATH);
            booking.setRemindedAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }
    }

    // --- confirmation -------------------------------------------------------

    @Transactional(readOnly = true)
    public MentoringBookingDTO getConfirmDetails(String rawToken) {
        return MentoringBookingDTO.forConfirmation(validate(rawToken));
    }

    @Transactional
    public void confirm(String rawToken) {
        MentoringBooking booking = validate(rawToken);
        if (booking.getState() == BookingState.BOOKED) {
            markConfirmed(booking, OffsetDateTime.now());
        }
    }

    private MentoringBooking validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new BadRequestException(LINK_FAILED);
        MentoringBooking booking = bookingRepository.findByConfirmTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException(LINK_FAILED));
        // Only a live booking can be confirmed: released or cancelled ones are done.
        if (booking.getState() == BookingState.CONFIRMED) return booking;
        OffsetDateTime now = OffsetDateTime.now();
        if (booking.getState() != BookingState.BOOKED || booking.getConfirmSentAt() == null
                || !now.isBefore(confirmationDeadline(booking)) || !now.isBefore(booking.getSlot().getStartsAt())) {
            throw new BadRequestException(LINK_FAILED);
        }
        return booking;
    }

    private void markConfirmed(MentoringBooking booking, OffsetDateTime now) {
        booking.setState(BookingState.CONFIRMED);
        booking.setConfirmedAt(now.toLocalDateTime());
        bookingRepository.save(booking);
    }

    static boolean hasTimeForConfirmation(OffsetDateTime startsAt, OffsetDateTime now) {
        return hasRebookingTime(startsAt, confirmationDeadline(startsAt, now));
    }

    private static boolean hasRebookingTime(OffsetDateTime startsAt, OffsetDateTime now) {
        return now.isBefore(startsAt.minusHours(REBOOKING_BUFFER_HOURS));
    }

    private static OffsetDateTime confirmationDeadline(MentoringBooking booking) {
        OffsetDateTime sentAt = booking.getConfirmSentAt().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        return confirmationDeadline(booking.getSlot().getStartsAt(), sentAt);
    }

    private static OffsetDateTime confirmationDeadline(OffsetDateTime startsAt, OffsetDateTime sentAt) {
        OffsetDateTime usualDeadline = startsAt.minusHours(RELEASE_LEAD_HOURS);
        OffsetDateTime gracePeriodEnd = sentAt.plusHours(CONFIRMATION_GRACE_HOURS);
        return usualDeadline.isAfter(gracePeriodEnd) ? usualDeadline : gracePeriodEnd;
    }

    // --- helpers ------------------------------------------------------------

    private void invite(User recipient, String subject, String body, String sessionPath,
                        MentoringBooking booking, boolean cancelled) {
        String sessionPage = frontendUrl + sessionPath;
        String email = recipient.getEmail();
        Long bookingId = booking.getId();
        EmailAttachment ics = CalendarInvite.forSession(booking, email, mailFrom, sessionPage, cancelled);
        afterCommit(() -> {
            try {
                emailSender.send(email, subject, body + "\n\n" + sessionPage, ics);
            } catch (Exception e) {
                log.warn("Could not email about booking {}: {}", bookingId, e.getMessage());
            }
        });
    }

    private void afterCommit(Runnable send) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send.run();
            }
        });
    }

    private void safeSend(String to, String subject, String body) {
        try {
            emailSender.send(to, subject, body);
        } catch (Exception e) {
            log.warn("Could not email {}: {}", to, e.getMessage());
        }
    }

    private String when(MentoringSlot slot) {
        return formatTime(slot.getStartsAt());
    }

    private String formatTime(OffsetDateTime time) {
        return WHEN.format(time.atZoneSameInstant(ZoneId.of(timezone)));
    }

    private String track(MentoringSlot slot) {
        return slot.getTrack().name().toLowerCase().replace('_', ' ');
    }

    private String name(User user) {
        return user.getFirstName() + " " + user.getLastName();
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
