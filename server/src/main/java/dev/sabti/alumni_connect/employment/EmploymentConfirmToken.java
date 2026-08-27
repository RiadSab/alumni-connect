package dev.sabti.alumni_connect.employment;

import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// One-time link behind the yearly nudge. Hashed, single-use and time-limited like the claim token.
@Entity
@Table(name = "employment_confirm_tokens")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmploymentConfirmToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employment_entry_id", nullable = false)
    private EmploymentEntry employmentEntry;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;
}
