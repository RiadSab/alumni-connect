package dev.sabti.alumni_connect.mentoring;

import java.time.OffsetDateTime;

// When the meeting link is handed over: ten minutes before the start, until the session is over.
// Outside that window neither party is given the address, so a link can't be passed around.
final class JoinWindow {
    private JoinWindow() {}

    private static final int OPENS_MINUTES_BEFORE = 10;
    private static final int CLOSES_MINUTES_AFTER = 30;

    static boolean isOpen(MentoringSlot slot) {
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(slot.getStartsAt().minusMinutes(OPENS_MINUTES_BEFORE))
                && now.isBefore(slot.getStartsAt().plusMinutes(slot.getDurationMinutes() + CLOSES_MINUTES_AFTER));
    }
}
