package dev.sabti.alumni_connect.employment;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// One period: a job, a course of study, or a stretch of looking. An open period (endedAt null)
// is the alumnus's current situation.
@Entity
@Table(name = "employment_entries")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmploymentEntry extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentStatus status;

    // Employer and job title are required for EMPLOYED, meaningless for the other two.
    private String employer;

    private String jobTitle;

    private String sector;

    private String city;

    @Column(nullable = false)
    private LocalDate startedAt;

    private LocalDate endedAt;

    // Set when the alumnus answers the yearly nudge or edits the entry: how stale the row is.
    private LocalDateTime lastConfirmedAt;
}
