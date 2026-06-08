package dev.sabti.alumni_connect.job;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// company and postedBy are never accepted here — both are derived from the
// authenticated poster's own CompanyUserProfile, the same server-decides-identity
// reasoning as RegisterCompanyDTO excluding companyRole/companyId.
@Data
public class CreateJobOfferDTO {

    @NotBlank(message = "Field 'title' is required")
    private String title;

    private String description;
    private List<String> requirements;
    private JobCity city;
    private Integer minSalary;
    private Integer maxSalary;
    private EmploymentType employmentType;
    private LocalDateTime applicationDeadline;
    private Integer experienceYears;
    private List<String> skillsRequired;
    private Boolean isRemote;
    private Boolean isUrgent;
    private Integer maxApplications;
    private String contactEmail;
}
