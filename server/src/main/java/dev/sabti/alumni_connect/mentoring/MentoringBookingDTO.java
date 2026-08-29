package dev.sabti.alumni_connect.mentoring;

// A booking as seen by one of its two parties. meetingLink is filled in only for them.
public record MentoringBookingDTO(Long id, BookingState state, MentoringSlotDTO slot,
                                  String studentName, String meetingLink) {

    public static MentoringBookingDTO from(MentoringBooking booking, boolean withLink) {
        var student = booking.getStudent().getUser();
        return new MentoringBookingDTO(booking.getId(), booking.getState(),
                MentoringSlotDTO.from(booking.getSlot()),
                student.getFirstName() + " " + student.getLastName(),
                withLink ? booking.getSlot().getMeetingLink() : null);
    }
}
