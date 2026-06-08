package dev.sabti.alumni_connect.company.entities;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

// Company-user-specific data, related to a User by composition (mirrors CandidateProfile).
// Lives in the company feature rather than auth — Company already needs Fields from auth,
// so keeping the dependency one-directional (company -> auth) avoids a package cycle.
@Entity
@Table(name = "company_user_profiles")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyUserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Platform permission level — set by the registration flow (OWNER for the company's
    // first user) and by the company's own OWNER/admins for everyone who joins after.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyRole companyRole = CompanyRole.MEMBER;

    @Enumerated(EnumType.STRING)
    private CompanyUserPosition position;
}
