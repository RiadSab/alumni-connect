package dev.sabti.alumni_connect.job.offers;

import dev.sabti.alumni_connect.job.entities.EmploymentType;
import dev.sabti.alumni_connect.job.entities.JobCity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// Optional filters for the public offers search; null/empty means "don't filter on this".
@Getter
@AllArgsConstructor
public class JobOfferSearchCriteria {
    private final String q;                 // case-insensitive substring match on title
    private final JobCity city;
    private final EmploymentType employmentType;
    private final Boolean isRemote;
    private final List<String> skills;      // offers requiring ANY of these skills
}
