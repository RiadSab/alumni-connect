package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.Priority;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

// Flattens jobOffer/applicant/reviewedBy to id + display name; reviewedBy is null until reviewed.
@Data
public class JobApplicationDTO {
    private Long id;
    private Long jobOfferId;
    private String jobOfferTitle;
    private Long applicantId;
    private String applicantName;
    private String resumeStorageId;
    private Set<String> certificatesStorageIds;
    private String coverLetter;
    private ApplicationStatus applicationStatus;
    private LocalDateTime reviewedAt;
    private Long reviewedById;
    private String reviewedByName;
    private String companyUserNote;
    private Priority priority;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobApplicationDTO from(JobApplication application) {
        JobApplicationDTO dto = new JobApplicationDTO();
        dto.id = application.getId();
        dto.jobOfferId = application.getJobOffer().getId();
        dto.jobOfferTitle = application.getJobOffer().getTitle();
        dto.applicantId = application.getApplicant().getId();
        dto.applicantName = application.getApplicant().getUser().getFirstName() + " " + application.getApplicant().getUser().getLastName();
        dto.resumeStorageId = application.getResumeStorageId();
        dto.certificatesStorageIds = application.getCertificatesStorageIds();
        dto.coverLetter = application.getCoverLetter();
        dto.applicationStatus = application.getApplicationStatus();
        dto.reviewedAt = application.getReviewedAt();
        if (application.getReviewedBy() != null) {
            dto.reviewedById = application.getReviewedBy().getId();
            dto.reviewedByName = application.getReviewedBy().getUser().getFirstName() + " " + application.getReviewedBy().getUser().getLastName();
        }
        dto.companyUserNote = application.getCompanyUserNote();
        dto.priority = application.getPriority();
        dto.rating = application.getRating();
        dto.createdAt = application.getCreatedAt();
        dto.updatedAt = application.getUpdatedAt();
        return dto;
    }
}
