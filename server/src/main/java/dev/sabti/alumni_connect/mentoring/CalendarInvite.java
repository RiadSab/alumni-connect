package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.shared.email.EmailAttachment;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// A minimal iCalendar file — the thing that actually drops a session into someone's calendar with
// the reminder set. Hand-written rather than a library: it is fifteen lines of text.
//
// Each party gets their own invite listing only themselves. Putting both on it would hand each of
// them the other's email address, which is exactly what the in-app flow exists to avoid.
final class CalendarInvite {
    private CalendarInvite() {}

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    static EmailAttachment forSession(MentoringBooking booking, String recipientEmail,
                                      String organiser, String sessionPage, boolean cancelled) {
        MentoringSlot slot = booking.getSlot();
        OffsetDateTime start = slot.getStartsAt().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(slot.getDurationMinutes());

        String body = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Alumni Connect//Mentoring//EN
                METHOD:%s
                BEGIN:VEVENT
                UID:mentoring-%d@alumni-connect
                SEQUENCE:%d
                DTSTAMP:%s
                DTSTART:%s
                DTEND:%s
                SUMMARY:%s
                DESCRIPTION:%s
                LOCATION:%s
                STATUS:%s
                ORGANIZER:mailto:%s
                ATTENDEE;ROLE=REQ-PARTICIPANT:mailto:%s
                BEGIN:VALARM
                TRIGGER:-PT30M
                ACTION:DISPLAY
                DESCRIPTION:Mock interview in 30 minutes
                END:VALARM
                END:VEVENT
                END:VCALENDAR
                """.formatted(
                cancelled ? "CANCEL" : "REQUEST",
                booking.getId(),
                // A cancellation must outrank the invite it replaces, or calendars ignore it.
                cancelled ? 1 : 0,
                STAMP.format(OffsetDateTime.now(ZoneOffset.UTC)),
                STAMP.format(start),
                STAMP.format(end),
                escape("Mock interview · " + label(slot.getTrack())),
                escape("Twenty minutes: an exercise, then feedback."),
                escape(sessionPage),
                cancelled ? "CANCELLED" : "CONFIRMED",
                organiser,
                recipientEmail);

        return new EmailAttachment("session.ics", "text/calendar",
                body.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static String label(MentoringTrack track) {
        return track.name().toLowerCase().replace('_', ' ');
    }

    // Commas, semicolons and backslashes are field separators in iCalendar text values.
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,");
    }
}
