package dev.sabti.alumni_connect.auth.register;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.register.validation.StudentIdRequired;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

// File uploads (resume/profile photo) are deliberately left out for now — they'll be added
// once the file-storage feature exists, instead of dragging that dependency in here.
@Data
@StudentIdRequired
public class RegisterCandidateDTO {

    @NotBlank(message = "Field 'firstName' is required")
    private String firstName;

    @NotBlank(message = "Field 'lastName' is required")
    private String lastName;

    @NotBlank(message = "Field 'email' is required")
    @Email(message = "Field 'email' must be a valid email")
    private String email;

    @NotBlank(message = "Field 'password' is required")
    private String password;

    @NotBlank(message = "Field 'phoneNumber' is required")
    private String phoneNumber;

    @NotNull(message = "Field 'isStudent' is required")
    private Boolean isStudent;

    // Required only when isStudent is true — checked by @StudentIdRequired above
    private String studentId;

    @NotNull(message = "Field 'fieldOfStudy' is required")
    private Fields fieldOfStudy;

    @NotNull(message = "Field 'graduationYear' is required")
    private Integer graduationYear;

    private Set<@NotBlank(message = "Skills cannot be blank") String> skills = new HashSet<>();

    private String currentJobTitle;
    private String currentCompany;

    @Min(value = 0, message = "Experience years must be non-negative")
    private Integer experienceYears;

    @Pattern(regexp = "^(https?://.*)?$", message = "linkedinUrl invalide")
    private String linkedinUrl;

    @Pattern(regexp = "^(https?://.*)?$", message = "githubUrl invalide")
    private String githubUrl;

    @Pattern(regexp = "^(https?://.*)?$", message = "portfolioUrl invalide")
    private String portfolioUrl;

    private String bio;
}
