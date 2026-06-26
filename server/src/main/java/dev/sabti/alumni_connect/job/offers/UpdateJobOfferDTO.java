package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// All fields nullable = "leave unchanged"; status is included so close/reopen is a PATCH.
@Data
public class UpdateJobOfferDTO {
    private String title;
    private String description;
    private List<String> requirements;
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
    private String contactEmail;
}
