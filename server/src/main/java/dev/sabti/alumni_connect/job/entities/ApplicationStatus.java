package dev.sabti.alumni_connect.job.entities;

public enum ApplicationStatus {
    APPLIED,
    UNDER_REVIEW,
    SCHEDULED_INTERVIEW,
    INTERVIEWED,
    REJECTED,
    ACCEPTED,
    // Set by the candidate withdrawing their own application (the only status they control); every
    // other status is a company review outcome.
    WITHDRAWN
}
