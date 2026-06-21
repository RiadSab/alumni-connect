package dev.sabti.alumni_connect.auth.entities;

import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// A one-time password-reset code. The code is stored hashed (never plaintext); single-use via
// usedAt, time-limited via expiresAt, and brute-force-limited via attempts.
@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PasswordResetToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    @Column(nullable = false)
    private int attempts = 0;
}
