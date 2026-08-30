package dev.sabti.alumni_connect.notification;

// Candidate-facing events. Adding one means a migration: the column is CHECK-constrained.
public enum NotificationType {
    APPLICATION_ACCEPTED,
    APPLICATION_REJECTED,
    INTERVIEW_SCHEDULED,
    MENTORING_SLOT_BOOKED,
    MENTORING_SESSION_CANCELLED,
    MENTORING_SESSION_RELEASED
}
