package dev.sabti.alumni_connect.candidate;

import dev.sabti.alumni_connect.auth.entities.Fields;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

// All fields nullable: null means "leave unchanged", same pattern as UpdateJobOfferDTO/
// ReviewApplicationDTO. Identity fields (email, password, userStatus, userType) are
// deliberately not here — those go through auth/admin flows, not self-profile edits.
@Data
public class UpdateCandidateProfileDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Boolean isStudent;
    private String studentId;
    private Fields fieldOfStudy;
    private Integer graduationYear;
    private LocalDate dateOfBirth;
    private Set<String> skills;
    private String currentJobTitle;
    private String currentCompany;
    private Integer experienceYears;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String bio;
}
