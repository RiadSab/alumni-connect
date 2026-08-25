package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.InterviewMode;
import dev.sabti.alumni_connect.job.entities.Priority;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.time.OffsetDateTime;

// Every field nullable = "leave unchanged"; reviewedAt/reviewedBy are derived server-side.
@Data
public class ReviewApplicationDTO {
    private ApplicationStatus applicationStatus;
    private String companyUserNote;
    private Priority priority;

    @Max(10)
    private Integer rating;

    // Read only when applicationStatus is SCHEDULED_INTERVIEW; validated in the service, since the
    // required fields depend on interviewMode.
    private InterviewMode interviewMode;
    private OffsetDateTime interviewAt;
    private String interviewLink;
    private String interviewLocation;
    private String interviewerName;
}
