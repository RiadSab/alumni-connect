package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;

import java.util.List;

// The optional filters for the public offers search. Every field nullable/empty means "don't
// filter on this" — the controller binds them from @RequestParam, and JobOfferSpecs turns only
// the present ones into predicates. status=OPEN is NOT here: it's a non-optional invariant of
// the public list, enforced in the service, not a caller-supplied filter.
public record JobOfferSearchCriteria(
        String q,                       // case-insensitive substring match on title
        JobCity city,
        EmploymentType employmentType,
        Boolean isRemote,
        List<String> skills             // offers requiring ANY of these skills
) {}
