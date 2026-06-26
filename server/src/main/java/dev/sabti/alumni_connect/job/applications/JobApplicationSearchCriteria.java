package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Optional triage filters for one offer's applicants; each null means "don't filter on this".
@Getter
@AllArgsConstructor
public class JobApplicationSearchCriteria {
    private final ApplicationStatus status;
    private final Boolean reviewed;      // true = already reviewed (reviewedAt set); false = awaiting review
    private final Integer minRating;     // applications with rating >= this (unrated ones are excluded)
}
