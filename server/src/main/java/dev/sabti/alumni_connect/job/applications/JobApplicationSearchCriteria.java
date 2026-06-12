package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Optional triage filters for the applicants of one offer. Each null means "don't filter on
// this". The offer itself isn't here — it's the non-optional base of the query (and an
// authority boundary), enforced in the service, not a caller-supplied filter.
@Getter
@AllArgsConstructor
public class JobApplicationSearchCriteria {
    private final ApplicationStatus status;
    private final Boolean reviewed;      // true = already reviewed (reviewedAt set); false = awaiting review
    private final Integer minRating;     // applications with rating >= this (unrated ones are excluded)
}
