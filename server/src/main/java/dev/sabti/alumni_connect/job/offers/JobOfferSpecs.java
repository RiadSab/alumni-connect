package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import dev.sabti.alumni_connect.job.entities.JobOffer;
import dev.sabti.alumni_connect.job.entities.JobStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

// Predicate builders for the public offers search; the service ANDs only the present filters.
final class JobOfferSpecs {
    private JobOfferSpecs() {}

    // The base scope: only OPEN offers are publicly browsable.
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

    // Offers requiring ANY of the given skills (case-insensitive); distinct() avoids duplicate rows.
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
