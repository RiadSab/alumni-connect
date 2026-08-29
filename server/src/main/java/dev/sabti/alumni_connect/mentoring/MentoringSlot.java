package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

// Supply: one slot an alumnus opened. Published by someone with a claimed roster row, so the
// mentor pool is the school's own list rather than whoever says they graduated.
@Entity
@Table(name = "mentoring_slots")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MentoringSlot extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_profile_id", nullable = false)
    private CandidateProfile mentor;

    // Timestamptz like the interview time: a student reading this from another country must not
    // see it an hour off.
    @Column(nullable = false)
    private OffsetDateTime startsAt;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MentoringTrack track;

    // Null means any filière.
    @Enumerated(EnumType.STRING)
    private Fields targetField;

    @Column(nullable = false, length = 512)
    private String meetingLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.OPEN;
}
