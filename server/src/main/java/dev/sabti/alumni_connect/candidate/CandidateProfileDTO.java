package dev.sabti.alumni_connect.candidate;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

// Flattens User identity fields together with CandidateProfile data — same shape for
// /me (self) and /{id} (admin review of a pending candidate).
@Data
public class CandidateProfileDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UserStatus userStatus;
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
    private String profilePhotoId;
    private String resumeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CandidateProfileDTO from(User user, CandidateProfile profile) {
        CandidateProfileDTO dto = new CandidateProfileDTO();
        dto.id = user.getId();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.email = user.getEmail();
        dto.phoneNumber = user.getPhoneNumber();
        dto.userStatus = user.getUserStatus();
        dto.isStudent = profile.getIsStudent();
        dto.studentId = profile.getStudentId();
        dto.fieldOfStudy = profile.getFieldOfStudy();
        dto.graduationYear = profile.getGraduationYear();
        dto.dateOfBirth = profile.getDateOfBirth();
        dto.skills = profile.getSkills();
        dto.currentJobTitle = profile.getCurrentJobTitle();
        dto.currentCompany = profile.getCurrentCompany();
        dto.experienceYears = profile.getExperienceYears();
        dto.linkedinUrl = profile.getLinkedinUrl();
        dto.githubUrl = profile.getGithubUrl();
        dto.portfolioUrl = profile.getPortfolioUrl();
        dto.bio = profile.getBio();
        dto.profilePhotoId = profile.getProfilePhotoId();
        dto.resumeId = profile.getResumeId();
        dto.createdAt = profile.getCreatedAt();
        dto.updatedAt = profile.getUpdatedAt();
        return dto;
    }
}
