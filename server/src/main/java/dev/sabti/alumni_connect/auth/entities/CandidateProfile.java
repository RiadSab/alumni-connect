package dev.sabti.alumni_connect.auth.entities;

import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

// Candidate-specific data, related to a User by composition (instead of Candidate extends User).
// Keeps auth/identity data out of this entity entirely — mapping it to a response DTO can be
// close to a 1:1 copy without risking leaking passwordHash/email/etc.
@Entity
@Table(name = "candidate_profiles")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CandidateProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Boolean isStudent = false;

    // Required only when isStudent is true — enforced by @StudentIdRequired on the registration
    // DTO; stays nullable here since the requirement is conditional, not a per-type DB constraint.
    private String studentId;

    @Enumerated(EnumType.STRING)
    private Fields fieldOfStudy;

    private Integer graduationYear;
    private LocalDate dateOfBirth;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "candidate_skills", joinColumns = @JoinColumn(name = "candidate_profile_id"))
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>();

    private String currentJobTitle;
    private String currentCompany;
    private Integer experienceYears;

    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String bio;
    private String profilePhotoId;
    private String resumeId;
}
