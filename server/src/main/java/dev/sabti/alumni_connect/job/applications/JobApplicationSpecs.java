package dev.sabti.alumni_connect.job.applications;

import dev.sabti.alumni_connect.job.entities.ApplicationStatus;
import dev.sabti.alumni_connect.job.entities.JobApplication;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import org.springframework.data.jpa.domain.Specification;

// Predicate builders for triaging one offer's applicants; the service ANDs only the supplied filters.
final class JobApplicationSpecs {
    private JobApplicationSpecs() {}

    // The base scope: applications belonging to this one offer.
    static Specification<JobApplication> forOffer(JobOffer offer) {
        return (root, query, cb) -> cb.equal(root.get("jobOffer").get("id"), offer.getId());
    }

    static Specification<JobApplication> hasStatus(ApplicationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("applicationStatus"), status);
    }

    // reviewedAt is set by review(); its presence is the source of truth for "has been reviewed".
    static Specification<JobApplication> isReviewed(boolean reviewed) {
        return (root, query, cb) -> reviewed
                ? cb.isNotNull(root.get("reviewedAt"))
                : cb.isNull(root.get("reviewedAt"));
    }

    // Unrated applications (null rating) never satisfy a minRating filter — intended.
    static Specification<JobApplication> minRating(int minRating) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), minRating);
    }
}
