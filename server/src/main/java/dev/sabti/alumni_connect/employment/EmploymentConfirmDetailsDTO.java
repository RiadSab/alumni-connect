package dev.sabti.alumni_connect.employment;

import java.time.LocalDate;

// What the confirm page shows: enough for the person to recognise the entry, nothing more.
public record EmploymentConfirmDetailsDTO(String firstName, EmploymentStatus status, String employer,
                                          String jobTitle, LocalDate startedAt) {
}
