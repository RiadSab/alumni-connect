package dev.sabti.alumni_connect.employment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmploymentEntryDTO(Long id, EmploymentStatus status, String employer, String jobTitle,
                                 String sector, String city, LocalDate startedAt, LocalDate endedAt,
                                 LocalDateTime lastConfirmedAt) {

    public static EmploymentEntryDTO from(EmploymentEntry entry) {
        return new EmploymentEntryDTO(entry.getId(), entry.getStatus(), entry.getEmployer(),
                entry.getJobTitle(), entry.getSector(), entry.getCity(), entry.getStartedAt(),
                entry.getEndedAt(), entry.getLastConfirmedAt());
    }
}
