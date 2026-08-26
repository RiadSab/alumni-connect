package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// One graduate on the school's list. Imported from the school's own CSV, so the promotion and
// field are trusted facts rather than something the person typed about themselves.
@Entity
@Table(name = "alumni_records")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AlumniRecord extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String studentId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Fields fieldOfStudy;

    @Column(nullable = false)
    private Integer promotionYear;

    // The school has no address for everyone; those rows still count in the denominator.
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by_user_id", unique = true)
    private User claimedBy;

    private LocalDateTime claimedAt;

    private LocalDateTime optedOutAt;
}
