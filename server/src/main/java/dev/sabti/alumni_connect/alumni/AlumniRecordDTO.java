package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.Fields;

import java.time.LocalDateTime;

public record AlumniRecordDTO(Long id, String studentId, String firstName, String lastName,
                              Fields fieldOfStudy, Integer promotionYear, String email,
                              boolean claimed, LocalDateTime claimedAt, LocalDateTime optedOutAt) {

    public static AlumniRecordDTO from(AlumniRecord record) {
        return new AlumniRecordDTO(record.getId(), record.getStudentId(), record.getFirstName(),
                record.getLastName(), record.getFieldOfStudy(), record.getPromotionYear(),
                record.getEmail(), record.getClaimedBy() != null, record.getClaimedAt(),
                record.getOptedOutAt());
    }
}
