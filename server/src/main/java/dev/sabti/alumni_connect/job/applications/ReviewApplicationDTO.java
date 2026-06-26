package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.Priority;
import jakarta.validation.constraints.Max;
import lombok.Data;

// Every field nullable = "leave unchanged"; reviewedAt/reviewedBy are derived server-side.
@Data
public class ReviewApplicationDTO {
    private ApplicationStatus applicationStatus;
    private String companyUserNote;
    private Priority priority;

    @Max(10)
    private Integer rating;
}
