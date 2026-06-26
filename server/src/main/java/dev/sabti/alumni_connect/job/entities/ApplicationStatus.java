package dev.sabti.alumni_connect.job.entities;

public enum ApplicationStatus {
    APPLIED,
    UNDER_REVIEW,
    SCHEDULED_INTERVIEW,
    INTERVIEWED,
    REJECTED,
    ACCEPTED,
    // Set by the candidate; every other status is a company review outcome.
    WITHDRAWN
}
