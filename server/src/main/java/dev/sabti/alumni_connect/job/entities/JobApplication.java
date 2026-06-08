package dev.sabti.alumni_connect.job.entities;

import dev.sabti.alumni_connect.auth.entities.CandidateProfile;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "job_applications")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JobApplication extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private CandidateProfile applicant;

    // Resume/certificates reference file-storage IDs — stay nullable until that
    // feature exists, the same "deferred until file-storage exists" treatment as
    // CandidateProfile.resumeId / Company.logoId.
    private String resumeStorageId;

    @ElementCollection
    @CollectionTable(name = "job_application_certificates", joinColumns = @JoinColumn(name = "job_application_id"))
    private Set<String> certificatesStorageIds;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus applicationStatus = ApplicationStatus.APPLIED;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private CompanyUserProfile reviewedBy;

    @Column(columnDefinition = "TEXT")
    private String companyUserNote;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Max(10)
    private Integer rating;
}
