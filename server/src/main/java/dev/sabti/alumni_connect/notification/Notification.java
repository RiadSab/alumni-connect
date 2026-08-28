package dev.sabti.alumni_connect.notification;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// One thing that happened to one user. The sentence is rendered client-side from the type, so a
// notification reads in whichever language the reader has picked.
@Entity
@Table(name = "notifications")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // What it's about (a job title) and where (a company).
    private String subject;

    private String context;

    // Relative path the client navigates to when it's clicked.
    private String link;

    // Null until clicked: unread is what the dashboard shows.
    private LocalDateTime readAt;
}
