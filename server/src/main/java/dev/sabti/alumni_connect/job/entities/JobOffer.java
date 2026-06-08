package dev.sabti.alumni_connect.job.entities;

import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyUserProfile;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "job_offers")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JobOffer extends BaseEntity {
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "job_requirements", joinColumns = @JoinColumn(name = "job_offer_id"))
    private List<String> requirements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // The company-side user who posted this offer — kept distinct from `company`
    // so a listing always traces back to an accountable person, the same reasoning
    // that shaped the company-registration vertical.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by_id", nullable = false)
    private CompanyUserProfile postedBy;

    @Enumerated(EnumType.STRING)
    private JobCity city;

    private Integer minSalary;
    private Integer maxSalary;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    private LocalDateTime applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.OPEN;

    private Integer experienceYears;

    @ElementCollection
    @CollectionTable(name = "job_skills_required", joinColumns = @JoinColumn(name = "job_offer_id"))
    private List<String> skillsRequired;

    private Boolean isRemote = false;
    private Boolean isUrgent = false;

    private Integer maxApplications;
    private Integer currentApplicationCount = 0;

    private String contactEmail;
}
