package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// Flattens company/postedBy down to id + display name — callers never need the full
// nested Company/CompanyUserProfile/User graph (and never get a Hibernate lazy-proxy
// or passwordHash leaking through it). One shape for both list and detail views.
@Data
public class JobOfferDTO {
    private Long id;
    private String title;
    private String description;
    private List<String> requirements;
    private Long companyId;
    private String companyName;
    private Long postedById;
    private String postedByName;
    private JobCity city;
    private Integer minSalary;
    private Integer maxSalary;
    private EmploymentType employmentType;
    private LocalDateTime applicationDeadline;
    private JobStatus status;
    private Integer experienceYears;
    private List<String> skillsRequired;
    private Boolean isRemote;
    private Boolean isUrgent;
    private Integer maxApplications;
    private Integer currentApplicationCount;
    private String contactEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean hasApplied; // for the candidate
    private Boolean isSaved;    // for the candidate

    public static JobOfferDTO from(JobOffer offer) {
        JobOfferDTO dto = new JobOfferDTO();
        dto.id = offer.getId();
        dto.title = offer.getTitle();
        dto.description = offer.getDescription();
        dto.requirements = offer.getRequirements();
        dto.companyId = offer.getCompany().getId();
        dto.companyName = offer.getCompany().getName();
        dto.postedById = offer.getPostedBy().getId();
        dto.postedByName = offer.getPostedBy().getUser().getFirstName() + " " + offer.getPostedBy().getUser().getLastName();
        dto.city = offer.getCity();
        dto.minSalary = offer.getMinSalary();
        dto.maxSalary = offer.getMaxSalary();
        dto.employmentType = offer.getEmploymentType();
        dto.applicationDeadline = offer.getApplicationDeadline();
        dto.status = offer.getStatus();
        dto.experienceYears = offer.getExperienceYears();
        dto.skillsRequired = offer.getSkillsRequired();
        dto.isRemote = offer.getIsRemote();
        dto.isUrgent = offer.getIsUrgent();
        dto.maxApplications = offer.getMaxApplications();
        dto.currentApplicationCount = offer.getCurrentApplicationCount();
        dto.contactEmail = offer.getContactEmail();
        dto.createdAt = offer.getCreatedAt();
        dto.updatedAt = offer.getUpdatedAt();
        return dto;
    }
}
