package dev.sabti.alumni_connect.mentoring;

// The room URL is available to booking participants during their join window.
public record MentoringBookingDTO(Long id, BookingState state, MentoringSlotDTO slot,
                                  String studentName, String meetingLink) {

    static MentoringBookingDTO forParticipant(MentoringBooking booking) {
        return from(booking, JoinWindow.canJoin(booking) ? booking.getSlot().getMeetingLink() : null);
    }

    static MentoringBookingDTO forConfirmation(MentoringBooking booking) {
        return from(booking, null);
    }

    private static MentoringBookingDTO from(MentoringBooking booking, String meetingLink) {
        var student = booking.getStudent().getUser();
        return new MentoringBookingDTO(booking.getId(), booking.getState(),
                MentoringSlotDTO.from(booking.getSlot()),
                student.getFirstName() + " " + student.getLastName(),
                meetingLink);
    }
}
