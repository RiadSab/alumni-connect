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
import java.util.List;
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
    private static final int RELEASE_AFTER_HOURS = 12;
    private static final int REMINDER_MINUTES = 60;
    private static final String LINK_FAILED = "This link is invalid or has expired";

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
                        + "The joining link appears on your session page ten minutes before we start:\n"
                        + frontendUrl + "/mentoring",
                booking, false);
        invite(mentor, "A student booked one of your slots",
                name(student) + " booked your " + track(slot) + " slot on " + when + ".\n\n"
                        + frontendUrl + "/mentoring/slots",
                booking, false);

        notificationService.notify(mentor, NotificationType.MENTORING_SLOT_BOOKED,
                track(slot), name(student), "/mentoring/slots");
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
                booking, true);
        invite(mentor, "A mock interview was cancelled",
                (byMentor ? "You cancelled" : name(student) + " cancelled") + " the session on " + when + ".",
                booking, true);

        User other = byMentor ? student : mentor;
        notificationService.notify(other, NotificationType.MENTORING_SESSION_CANCELLED,
                track(slot), when, byMentor ? "/mentoring" : "/mentoring/slots");
    }

    // --- sweeps -------------------------------------------------------------

    // Hourly rather than daily: a session booked 20 hours ahead still gets asked to confirm.
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void askForConfirmations() {
        OffsetDateTime now = OffsetDateTime.now();
        for (MentoringBooking booking : bookingRepository.findAwaitingConfirmation(now, now.plusHours(CONFIRM_ASKED_HOURS))) {
            try {
                String raw = issueConfirmToken(booking);
                emailSender.send(booking.getStudent().getUser().getEmail(),
                        "Confirm your mock interview",
                        "Your session with " + name(booking.getSlot().getMentor().getUser())
                                + " is on " + when(booking.getSlot()) + ".\n\n"
                                + "Confirm you're coming, or the slot goes back to another student:\n"
                                + frontendUrl + "/mentoring/confirm/" + raw);
                booking.setConfirmSentAt(LocalDateTime.now());
                bookingRepository.save(booking);
            } catch (Exception e) {
                log.warn("Could not ask for confirmation on booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 10 * * * *")
    @Transactional
    public void releaseUnconfirmed() {
        OffsetDateTime cutoff = OffsetDateTime.now().plusHours(RELEASE_AFTER_HOURS);
        for (MentoringBooking booking : bookingRepository.findUnconfirmed(cutoff)) {
            MentoringSlot slot = booking.getSlot();
            booking.setState(BookingState.RELEASED_UNCONFIRMED);
            booking.setCancelledAt(LocalDateTime.now());
            bookingRepository.save(booking);

            // Back in the pool if there is still time for someone else to take it.
            if (slot.getStatus() == SlotStatus.BOOKED && slot.getStartsAt().isAfter(OffsetDateTime.now())) {
                slot.setStatus(SlotStatus.OPEN);
                slotRepository.save(slot);
            }

            User student = booking.getStudent().getUser();
            safeSend(student.getEmail(), "Your mock interview slot was released",
                    "You didn't confirm the session on " + when(slot) + ", so it went back to another student.\n\n"
                            + "You can book another one:\n" + frontendUrl + "/mentoring");
            notificationService.notify(student, NotificationType.MENTORING_SESSION_RELEASED,
                    track(slot), when(slot), "/mentoring");
            safeSend(slot.getMentor().getUser().getEmail(), "A slot was released",
                    "The student didn't confirm, so your " + when(slot) + " slot is open again.");
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void sendReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        for (MentoringBooking booking : bookingRepository.findDueReminder(now, now.plusMinutes(REMINDER_MINUTES))) {
            MentoringSlot slot = booking.getSlot();
            String body = "Your mock interview starts at " + when(slot) + ".\n\n"
                    + "Join here: " + slot.getMeetingLink();
            safeSend(booking.getStudent().getUser().getEmail(), "Your mock interview is in an hour", body);
            safeSend(slot.getMentor().getUser().getEmail(), "Your mock interview is in an hour", body);
            booking.setRemindedAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }
    }

    // --- confirmation -------------------------------------------------------

    @Transactional(readOnly = true)
    public MentoringBookingDTO getConfirmDetails(String rawToken) {
        return MentoringBookingDTO.from(validate(rawToken), false);
    }

    @Transactional
    public void confirm(String rawToken) {
        MentoringBooking booking = validate(rawToken);
        booking.setState(BookingState.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    private MentoringBooking validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new BadRequestException(LINK_FAILED);
        MentoringBooking booking = bookingRepository.findByConfirmTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException(LINK_FAILED));
        // Only a live booking can be confirmed: released or cancelled ones are done.
        if (booking.getState() != BookingState.BOOKED && booking.getState() != BookingState.CONFIRMED) {
            throw new BadRequestException(LINK_FAILED);
        }
        return booking;
    }

    private String issueConfirmToken(MentoringBooking booking) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        booking.setConfirmTokenHash(hash(raw));
        return raw;
    }

    // --- helpers ------------------------------------------------------------

    private void invite(User recipient, String subject, String body, MentoringBooking booking, boolean cancelled) {
        EmailAttachment ics = CalendarInvite.forSession(booking, recipient.getEmail(), mailFrom, cancelled);
        try {
            emailSender.send(recipient.getEmail(), subject, body, ics);
        } catch (Exception e) {
            log.warn("Could not email {} about booking {}: {}", recipient.getId(), booking.getId(), e.getMessage());
        }
    }

    private void safeSend(String to, String subject, String body) {
        try {
            emailSender.send(to, subject, body);
        } catch (Exception e) {
            log.warn("Could not email {}: {}", to, e.getMessage());
        }
    }

    private String when(MentoringSlot slot) {
        return WHEN.format(slot.getStartsAt().atZoneSameInstant(ZoneId.of(timezone)));
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
