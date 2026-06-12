package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

// Predicate builders for the public offers search. Each returns a Specification for one filter;
// the service AND-combines only the ones whose criteria are present, so absent filters simply
// don't constrain the query (no "IS NULL OR ..." noise).
final class JobOfferSpecs {
    private JobOfferSpecs() {}

    // The non-optional base: only OPEN offers are publicly browsable (DRAFT/CLOSED/EXPIRED stay
    // hidden). Every search starts from this and ANDs the optional filters onto it.
    static Specification<JobOffer> isOpen() {
        return (root, query, cb) -> cb.equal(root.get("status"), JobStatus.OPEN);
    }

    static Specification<JobOffer> titleContains(String q) {
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }

    static Specification<JobOffer> hasCity(JobCity city) {
        return (root, query, cb) -> cb.equal(root.get("city"), city);
    }

    static Specification<JobOffer> hasEmploymentType(EmploymentType type) {
        return (root, query, cb) -> cb.equal(root.get("employmentType"), type);
    }

    static Specification<JobOffer> isRemote(Boolean remote) {
        return (root, query, cb) -> cb.equal(root.get("isRemote"), remote);
    }

    // Offers requiring ANY of the given skills. skillsRequired is an @ElementCollection, so this
    // joins the element table; distinct() prevents an offer from appearing once per matched skill.
    // Matching is case-insensitive because skillsRequired isn't normalized on write.
    static Specification<JobOffer> hasAnySkill(List<String> skills) {
        List<String> normalized = skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .toList();
        return (root, query, cb) -> {
            if (normalized.isEmpty()) return cb.conjunction();
            query.distinct(true);
            Join<JobOffer, String> skill = root.join("skillsRequired");
            return cb.lower(skill.as(String.class)).in(normalized);
        };
    }
}
