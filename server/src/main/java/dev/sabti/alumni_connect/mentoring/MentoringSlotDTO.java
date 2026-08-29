package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.auth.entities.Fields;

import java.time.OffsetDateTime;

// What a student sees when browsing. No meeting link: the room address belongs to whoever booked
// it, and a browsable list of live meeting rooms is a link anyone could walk into.
public record MentoringSlotDTO(Long id, String mentorName, Integer mentorPromotion,
                               MentoringTrack track, Fields targetField, OffsetDateTime startsAt,
                               Integer durationMinutes, SlotStatus status) {

    public static MentoringSlotDTO from(MentoringSlot slot) {
        var user = slot.getMentor().getUser();
        return new MentoringSlotDTO(slot.getId(),
                user.getFirstName() + " " + user.getLastName(),
                slot.getMentor().getGraduationYear(),
                slot.getTrack(), slot.getTargetField(), slot.getStartsAt(),
                slot.getDurationMinutes(), slot.getStatus());
    }
}
