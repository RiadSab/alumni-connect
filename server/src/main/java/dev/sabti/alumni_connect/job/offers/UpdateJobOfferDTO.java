package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// All fields nullable: null means "leave unchanged" — same pattern as
// ReviewApplicationDTO. status is included so closing/reopening an offer is
// just PATCH {"status": "CLOSED"}, no separate delete/close endpoint.
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
