package dev.sabti.alumni_connect.job;

import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.Priority;
import jakarta.validation.constraints.Max;
import lombok.Data;

// Every field is nullable and means "leave unchanged" — the caller only sends what
// it wants to change (e.g. just a status move, or just a rating). reviewedAt/reviewedBy
// are never accepted here: they're derived server-side from the authenticated reviewer.
@Data
public class ReviewApplicationDTO {
    private ApplicationStatus applicationStatus;
    private String companyUserNote;
    private Priority priority;

    @Max(10)
    private Integer rating;
}
