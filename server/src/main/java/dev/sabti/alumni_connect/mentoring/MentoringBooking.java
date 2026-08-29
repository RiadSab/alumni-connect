package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// One attempt at a slot. The row survives the slot going back in the pool, which is what the
// semester cap and the no-show count are read from.
@Entity
@Table(name = "mentoring_bookings")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MentoringBooking extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private MentoringSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private CandidateProfile student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingState state = BookingState.BOOKED;

    private LocalDateTime confirmedAt;

    private LocalDateTime cancelledAt;
}
