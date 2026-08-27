package dev.sabti.alumni_connect.employment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

// employer/jobTitle are checked in the service: they're required for EMPLOYED only.
@Data
public class SaveEmploymentEntryDTO {
    @NotNull(message = "Field 'status' is required")
    private EmploymentStatus status;

    private String employer;
    private String jobTitle;
    private String sector;
    private String city;

    @NotNull(message = "Field 'startedAt' is required")
    private LocalDate startedAt;

    // Null means this is where they are now.
    private LocalDate endedAt;
}
