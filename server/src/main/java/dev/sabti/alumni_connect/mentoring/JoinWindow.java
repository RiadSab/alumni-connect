package dev.sabti.alumni_connect.mentoring;

import java.time.OffsetDateTime;

// An active booking can get the room URL from ten minutes before start to thirty minutes after end.
final class JoinWindow {
    private JoinWindow() {}

    private static final int OPENS_MINUTES_BEFORE = 10;
    private static final int CLOSES_MINUTES_AFTER = 30;

    static boolean canJoin(MentoringBooking booking) {
        MentoringSlot slot = booking.getSlot();
        if (!BookingState.LIVE_STATES.contains(booking.getState()) || slot.getStatus() != SlotStatus.BOOKED) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(slot.getStartsAt().minusMinutes(OPENS_MINUTES_BEFORE))
                && now.isBefore(slot.getStartsAt().plusMinutes(slot.getDurationMinutes() + CLOSES_MINUTES_AFTER));
    }
}
