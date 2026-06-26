package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// company/postedBy are derived from the authenticated poster, never accepted here.
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
