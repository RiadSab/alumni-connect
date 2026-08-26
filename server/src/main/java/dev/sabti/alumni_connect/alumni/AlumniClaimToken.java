package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// A one-time claim link for one roster row. Stored as a SHA-256 hash so it can be looked up
// directly, single-use via usedAt, time-limited via expiresAt.
@Entity
@Table(name = "alumni_claim_tokens")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AlumniClaimToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumni_record_id", nullable = false)
    private AlumniRecord alumniRecord;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;
}
